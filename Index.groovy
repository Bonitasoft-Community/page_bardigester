import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.lang3.StringEscapeUtils
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import com.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.codehaus.groovy.tools.shell.CommandAlias;

import org.apache.commons.io.FileUtils;

import java.util.logging.Logger;
import org.json.simple.JSONArray;

public class Index implements PageController {
    static private final String defaultDropzone = ".";
    static private final String defaultArchivezone = defaultDropzone + "/archive";
    static private final String dropzoneProperty = "dropzone";
    static private final String archivezoneProperty = "archivezone";
    
    static private String dropzone = defaultDropzone;
    static private String archivezone = defaultArchivezone;

	Logger logger= Logger.getLogger("org.bonitasoft");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
        try {
            def String indexContent;
            pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter()

            String action = request.getParameter("action");
            logger.info("BARDigester runs action[" + action + "] from page=[" + request.getParameter("page") + "] !");
            if (action == null || action.length() == 0 ) {
                logger.info("BARDigester load Angular JS");
                runTheBonitaIndexDoGet(request, response, pageResourceProvider, pageContext);
                return;
            }

            ArrayList<HashMap<String, Object>> actions = new ArrayList<HashMap<String, Object>>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			
			// ------------------ getProperties
            if("getproperties".equals(action)) {
                logger.info("BARDigester gives properties");

                HashMap<String, Object> actionResult1 = new HashMap<String, Object>();
                actionResult1.put("name", "dropzone");

                HashMap<String, Object> actionResult2 = new HashMap<String, Object>();
                actionResult2.put("name", "archivezone");
				HashMap<String, Object> actionResult3 = new HashMap<String, Object>();

				// get the properties
				getProperties( pageResourceProvider, actionResult3 );
				
				// return it
                actionResult1.put("value", dropzone);
                actionResult2.put("value", archivezone);

                actions.add(actionResult1);
                actions.add(actionResult2);

                logger.info("BARDigester ends giving properties");
				
			// ---------------- setproperties
            } else if("setproperties".equals(action)) {
                logger.info("BARDigester setProperties");
			
				String dropzoneNewValue = request.getParameter(dropzoneProperty);
				String archivezoneNewValue = request.getParameter(archivezoneProperty);
                if(dropzoneNewValue != null && archivezoneNewValue != null && !dropzoneNewValue.equals(archivezoneNewValue)) {
					logger.info("BARDigester sets properties new Value["+dropzoneNewValue+"] archive["+archivezoneNewValue+"]");
					
					Properties properties = new Properties();
					
					HashMap<String, Object> actionResult = new HashMap<String, Object>();
					actionResult.put("name", "Set properties");
					actionResult.put("timestamp", dateFormat.format(new Date()));

					HashMap<String, Object> actionResult2 = new HashMap<String, Object>();
					actionResult2.put("name", "Set Drop Zone property");
					actionResult2.put("timestamp", dateFormat.format(new Date()));
					if (dropzoneNewValue == null  || dropzoneNewValue.isEmpty()) {
						actionResult2.put("status", "No new value found, it will set to: \"" + (new File(defaultDropzone)).getCanonicalPath() + "\"");
						properties.setProperty(dropzoneProperty, defaultDropzone);
					} else if(!(new File(dropzoneNewValue)).exists() || !(new File(dropzoneNewValue)).isDirectory()) {
						actionResult2.put("status", "New value path is not valid, it will set to: \"" + (new File(defaultDropzone)).getCanonicalPath() + "\"");
						properties.setProperty(dropzoneProperty, defaultDropzone);
					} else {
						actionResult2.put("status", "New value to be used: \"" + (new File(dropzoneNewValue)).getCanonicalPath() + "\"");
						properties.setProperty(dropzoneProperty, dropzoneNewValue);
					}
					actions.add(actionResult2);

					actionResult2 = new HashMap<String, Object>();
					actionResult2.put("name", "Set Archive Zone property");
					actionResult2.put("timestamp", dateFormat.format(new Date()));
					if (archivezoneNewValue == null  || archivezoneNewValue.isEmpty()) {
						actionResult2.put("status", "No new value found, it will set to: \"" + (new File(defaultArchivezone)).getCanonicalPath() + "\"");
						properties.setProperty(archivezoneProperty, defaultArchivezone);
					} else if(!(new File(archivezoneNewValue)).exists() || !(new File(archivezoneNewValue)).isDirectory()) {
						actionResult2.put("status", "New value path is not valid, it will set to: \"" + (new File(defaultArchivezone)).getCanonicalPath() + "\"");
						properties.setProperty(archivezoneProperty, defaultArchivezone);
					} else {
						actionResult2.put("status", "New value to be used: \"" + (new File(archivezoneNewValue)).getCanonicalPath() + "\"");
						properties.setProperty(archivezoneProperty, archivezoneNewValue);
					}
					actions.add(actionResult2);
					
					OutputStream output = null;
					try {
						output = new FileOutputStream(pageResourceProvider.getResourceAsFile("resources/conf/custompage.properties"));
						properties.store(output, null);
						actionResult.put("status", "Done");
						dropzone = properties.getProperty(dropzoneProperty);
						archivezone = properties.getProperty(archivezoneProperty);
					} catch (Exception e) {
						e.printStackTrace();
						actionResult.put("status", "Failure");
					} finally {
						if (output != null) {
							try {
								output.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}	
					actions.add(actionResult);
				} else {
					HashMap<String, Object> actionResult2 = new HashMap<String, Object>();
					actions.add(actionResult2);
				}
				
                logger.info("BARDigester ends setting properties");

			// ------------------ refresh				
            } else if ("refresh".equals(action))  {
                logger.info("BARDigester refreshes");

				HashMap<String, Object> actionResult3 = new HashMap<String, Object>();
				
				getProperties(pageResourceProvider, actionResult3);

				actionResult3.put("name", "Refresh LOG");
				actionResult3.put("timestamp", dateFormat.format(new Date()));
				actionResult3.put("status", "Dropzone file path is " + dropzone + " and Archive file path is " + archivezone);
				actions.add(actionResult3);
				
				
                String mPathOutput = archivezone;
                File pathOutputDir = new File(mPathOutput);
                boolean outputFolderIsHere = pathOutputDir.exists();
                if (!outputFolderIsHere) {
                    HashMap<String, Object> actionResult = new HashMap<String, Object>();
                    actionResult.put("name", "Create Archive folder (" + pathOutputDir.getCanonicalPath() + ")");
                    actionResult.put("timestamp", dateFormat.format(new Date()));
                    if(pathOutputDir.mkdirs()) {
                        actionResult.put("status", "Done");
                        outputFolderIsHere = true;
                    } else {
                        actionResult.put("status", "Failure");
                    }
                    actions.add(actionResult);
                }

                if (outputFolderIsHere) {
                    File dir = new File(dropzone);
                    File[] filesList = dir.listFiles();
                    for (File file : filesList) {
                        if (!file.isFile() || !file.getName().endsWith(".bar")) {
                            continue;
                        }
                        HashMap<String, Object> actionResult = new HashMap<String, Object>();
                        actionResult.put("name", "Handle Archive \"" + file.getCanonicalPath() + "\"");
                        actionResult.put("timestamp", dateFormat.format(new Date()));
                        try {
                            BusinessArchive businessArchive = BusinessArchiveFactory.readBusinessArchive(file);
                            if (businessArchive == null || businessArchive.getProcessDefinition() == null) {
                                actionResult.put("status", "Not an Bonita Archive File");
                                continue;
                            }

                            // delete an existing one ?
                            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(pageContext.getApiSession());
                            HashMap<String, Object> actionResult2 = new HashMap<String, Object>();
                            try {
                                Long processDefinitionId = processAPI.getProcessDefinitionId(businessArchive.getProcessDefinition().getName(), businessArchive.getProcessDefinition().getVersion());
								try
								{
									processAPI.disableProcess( processDefinitionId);
								}
								catch( Exception e)
								{};
                                processAPI.deleteProcessDefinition(processDefinitionId);
								
                                actionResult2.put("name", "Delete existant application");
                                actionResult2.put("timestamp", dateFormat.format(new Date()));
                                actionResult2.put("status", "Done");
                            } catch (ProcessDefinitionNotFoundException e) {
								logger.severe("barDisgester: the process does not exist before");
                                actionResult2.put("status", "New process");
							}
                                actions.add(actionResult2);

                            // now load the new bar file
                            ProcessDefinition processDefinition = processAPI.deploy(businessArchive);
                            processAPI.enableProcess(processDefinition.getId());

                            try {
                                // now move the path to the output directory
                                String outputFileName = file.getName();
                                Calendar c = Calendar.getInstance();
                                outputFileName = outputFileName.replace(".bar", "");
                                outputFileName = mPathOutput + "/" + outputFileName + "-" + c.get(Calendar.YEAR) + "_" + (c.get(Calendar.MONTH) + 1) + "_" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "_" + c.get(Calendar.MINUTE) + "_" +  + c.get(Calendar.SECOND) + "_" + c.get(Calendar.MILLISECOND) + ".bar";
                            
                                File outputFile = new File(outputFileName);
                                outputFile.createNewFile()
                                
                                String inputFileName = file.getCanonicalPath();
                            
                                OutputStream outStream = new FileOutputStream(outputFile);
                                InputStream inStream = new FileInputStream(file);
                                
                                byte[] buffer = new byte[1024];

                                int length;
                                // copy the file content in bytes
                                while ((length = inStream.read(buffer)) > 0) {
                                    outStream.write(buffer, 0, length);
                                }

                                inStream.close();
                                outStream.close();
                                
                                file.delete();
                            } catch (Exception e) {
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));
								final String exceptionDetails = sw.toString();
								logger.severe("barDisgester: Exception during move the archive  "+exceptionDetails);
							
                                actionResult.put("status", "Done but cannot move the archive to the output folder");
                            }

                            actionResult.put("status", "Done");
                        } catch (InvalidBusinessArchiveFormatException be) {
							StringWriter sw = new StringWriter();
							be.printStackTrace(new PrintWriter(sw));
							final String exceptionDetails = sw.toString();
						
                            logger.severe("Error on deployment "+be.toString()+" : "+exceptionDetails);
                            actionResult.put("status", "Invalid Business Archive Format "+exceptionDetails);
                        }   catch (Exception e) {
							StringWriter sw = new StringWriter();
							e.printStackTrace(new PrintWriter(sw));
							final String exceptionDetails = sw.toString();

                            logger.severe("Error on deployment "+exceptionDetails);
                            actionResult.put("status", "Failure : "+exceptionDetails);
                        }
                        actions.add(actionResult);
                    }

                }
                logger.info("BARDigester ends the refresh");
				
			// ------------------ upload				
            } else if ("uploadbar".equals(action))  {
                HashMap<String, Object> actionResult = new HashMap<String, Object>();
                actionResult.put("name", "Archive Upload");
                actionResult.put("timestamp", dateFormat.format(new Date()));

				String uploadedFile = request.getParameter("file");
				String uploadedFileName = request.getParameter("name"); 
				logger.info("BarDisgester : uploadFile ["+uploadedFile+"] fileName=["+ uploadedFileName+"]");
				
				
				// decode the full name of the uploaded file
				// pageDiretoru is \client\tenants\1\work\pages\custompage_bardigester
				// we look for \client\tenants\1\tmp
				File pageDirectory = pageResourceProvider.getPageDirectory();
				List<String> listParentTmpFile = new ArrayList<String>();
				listParentTmpFile.add( pageDirectory.getCanonicalPath()+"/../../../tmp/");
				listParentTmpFile.add( pageDirectory.getCanonicalPath()+"/../../");
				String completeBarFile=null;
				for (String pathTemp : listParentTmpFile)
				{
				logger.info("BarDisgester : CompleteuploadFile  TEST ["+pathTemp+uploadedFile+"]");	
					if (uploadedFile.length() > 0 && (new File(pathTemp+uploadedFile)).exists()) {
						completeBarFile=(new File(pathTemp+uploadedFile)).getAbsoluteFile() ;
						logger.info("BarDisgester : CompleteuploadFile  FOUND ["+completeBarFile+"]");					
					}
				}
				
				logger.info("BarDisgester : CompleteuploadFile ["+completeBarFile+"]");
				 
				getProperties(pageResourceProvider, actionResult);

								
				if ( completeBarFile==null) {
					actionResult.put("status", "Error: an error occurred during the upload request");
				} else {
					try {
					
						if(uploadedFile.endsWith(".bar")) {
							logger.info("BarDisgester : will copy file["+completeBarFile+"] to ["+ dropzone + File.separator + uploadedFileName+"]");

							FileUtils.copyFile(new File(completeBarFile), new File(dropzone + File.separator + uploadedFileName));
							logger.info("BarDisgester : copy file ["+completeBarFile+"] to ["+ dropzone + File.separator + uploadedFileName+"]");
							
						} else {
							actionResult.put("status", "Error: The uploaded file \"" + uploadedFile + "\" is not taken into account as it is not a BAR file");
						}
						
						// (new File(completeBarFile+uploadedFile)).delete();
						
						if(uploadedFile.endsWith(".bar")) {
							actionResult.put("status", "The BAR file \"" + uploadedFile + "\" has been uploaded in the drop zone directory");
						}
					} catch(Exception e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionDetails = sw.toString();
						logger.severe("BarDigesters : Exception ["+e.toString()+"] at "+exceptionDetails);
						actionResult.put("status", "Error: The BAR file \"" + uploadedFile + "\" has not been uploaded in the archive directory due to execution error");
					}
				}

                actions.add(actionResult);
            }

            out.write(JSONArray.toJSONString(actions));
            out.flush();
            out.close();
            return;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
        }
    }

    private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
        try {
            def String indexContent;
            pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s-> indexContent = s.getText() }

            def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";

            indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
            indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);

            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print(indexContent);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	private void getProperties(PageResourceProvider  pageResourceProvider, HashMap<String, Object> actionResult )
	{
		logger.info("barDisgester: loadproperties");
	
		try
		{
			Properties properties = new Properties();

			InputStream is = pageResourceProvider.getResourceAsStream("resources/conf/custompage.properties");
			properties.load(is);
			is.close();

			String temp = properties.get(dropzoneProperty);
			if(temp != null && !temp.isEmpty()) {
				dropzone = temp;
			}
			temp = properties.get(archivezoneProperty);
			if(temp != null && !temp.isEmpty()) {
				archivezone = temp;
			}
		} catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
        }
}
					

}
