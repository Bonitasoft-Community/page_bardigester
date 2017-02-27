'use strict';
/**
 * 
 */

(function() {

	if (typeof String.prototype.startsWith != 'function') {
		// see below for better implementation!
		String.prototype.startsWith = function (str){
			return this.indexOf(str) == 0;
		};
	}

	var appCommand = angular.module('bardigester', ['ngAnimate', 'ui.bootstrap', 'timer', 'toaster', 'angularFileUpload']);

//	Constant used to specify resource base path (facilitates integration into a Bonita custom page)
	appCommand.constant('RESOURCE_PATH', 'pageResource?page=custompage_bardigester&location=');

//	User app list controller
	appCommand.controller('DigesterController', function($rootScope, $scope, $upload, toaster) {
		var me = this;
		$scope.dropzonenewvalue = "";
		$scope.archivezonenewvalue = "";
		$scope.currentUploadFileIndex = 0;
		$scope.refreshisrunning = false;

		$rootScope.history = [];

		this.propop = function() {
			toaster.pop('success', "Properties have been set", "");
		};
		
		this.prodropzonepop = function() {
			toaster.pop('success', "Drop zone property has been set", "");
		};
		
		this.proarchivezonepop = function() {
			toaster.pop('success', "Archive zone property has been set", "");
		};

		this.proloadpop = function() {
			toaster.pop('success', "Properties have been loaded", "");
		};
		
		this.sameloadpop = function() {
			toaster.pop('error', "The drop zone and archive zone properties cannot be the same directory", "");
		};

		this.emptyrefreshpop = function() {
			toaster.pop('warning', "No archives have been digested", "");
		};

		this.createrefreshpop = function() {
			toaster.pop('success', "The archive directory has been generated", "");
		};
		
		this.fullrefreshpop = function() {
			toaster.pop('success', "Archive(s) has been digested", "");
		};

		this.errorpop = function() {
			toaster.pop('error', "An occurred error", "");
		};
		
		this.dropzonepropertyerrorpop = function() {
			toaster.pop('error', "The path of the drop zone is bad", "");
		};
		
		this.archivezonepropertyerrorpop = function() {
			toaster.pop('error', "The path of the archive zone is bad", "");
		};
		
		this.uploadsuccesspop = function() {
			toaster.pop('success', "A BAR file has been uploaded", "");
		};

		this.uploadwarningpop = function() {
			toaster.pop('warning', "A file has been avoided due to error", "");
		};
		
		this.uploaderrorpop = function() {
			toaster.pop('error', "An error occurred uploading a file in the archive directory", "");
		};
		
		this.flushToasts = function() {
			toaster.flush();
		};
		
		this.refreshfrombtn = function() {
			if(!$scope.refreshisrunning) {
				$scope.refreshisrunning = true;
			
				//flush the toasts
				me.flushToasts();
				$scope.$apply();
				$.ajax({
					method : 'GET',
					url : '?page=custompage_bardigester&action=refresh',			
					contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
					success : function (result) {
						var resultArray = JSON.parse(result)
						var arrayLength = resultArray.length;
						for (var i = 0; i < arrayLength; i++) {
							$rootScope.history.unshift(resultArray[i]);
						}
						if(arrayLength == 0) {
							me.emptyrefreshpop();
						} else if(resultArray[0].name.startsWith("Create Archive folder")) {
							me.createrefreshpop();
							if(arrayLength > 1) {
								me.fullrefreshpop();
							}
						} else {
							me.fullrefreshpop();
						}
						$scope.$apply();
					},
					error: function (result) {
						me.errorpop();
						$scope.$apply();
					}, complete: function() {
						$scope.refreshisrunning = false;
					}
				});
			}
		};

		this.autorefresh = function() {
			if(!$scope.refreshisrunning) {
				$scope.refreshisrunning = true;
				
				$.ajax({
					method : 'GET',
					url : '?page=custompage_bardigester&action=refresh',			
					contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
					success : function (result) {
						var resultArray = JSON.parse(result)
						var arrayLength = resultArray.length;
						for (var i = 0; i < arrayLength; i++) {
							$rootScope.history.unshift(resultArray[i]);
						}
						if(arrayLength != 0) {
							if(resultArray[0].name.startsWith("Create Archive folder")) {
								me.createrefreshpop();
								if(arrayLength > 1) {
									me.fullrefreshpop();
								}
							} else {
								me.fullrefreshpop();
							}
						}
						$scope.$apply();
					},
					error: function (result) {
						me.errorpop();
						$scope.$apply();
					},
					complete: function () {
						document.getElementsByTagName('timer')[0].addCDSeconds(60);
						$scope.refreshisrunning = false;
					}
				});
			} else {
				document.getElementsByTagName('timer')[0].addCDSeconds(60);
			}
		};

		this.getproperties = function(toast) {
			$.ajax({
				method : 'GET',
				url : '?page=custompage_bardigester&action=getproperties',			
				contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
				success : function (result) {
					var resultArray = JSON.parse(result);
					$scope.dropzonenewvalue = resultArray[0].value;
					$scope.archivezonenewvalue = resultArray[1].value;
					
					if(toast) {
						me.proloadpop();
					}
					$scope.$apply();
				},
				error: function (result) {
					me.errorpop();
					$scope.$apply();
				}
			});
		};

		this.setproperties = function() {
			//flush the toasts
			me.flushToasts();
			$scope.$apply();
			var url = '?page=custompage_bardigester&action=setproperties&dropzone=' + $scope.dropzonenewvalue + '&archivezone=' + $scope.archivezonenewvalue;
			$.ajax({
				method : 'GET',
				url : url,			
				contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
				success : function (result) {
					if(result != "[{}]") {
						var dropzoneerror = false;
						var archivezoneerror = false;
						var resultArray = JSON.parse(result);
						var arrayLength = resultArray.length;
						for (var i = 0; i < arrayLength; i++) {
							$rootScope.history.unshift(resultArray[i]);
							if(resultArray[i].status.toString().startsWith("New value path is not valid")) {
								if(i == 0) {
									dropzoneerror = true;
									me.dropzonepropertyerrorpop();
								} else if(i == 1) {
									archivezoneerror = true;
									me.archivezonepropertyerrorpop();
								}
								$scope.$apply();
							}
						}
						if(!dropzoneerror || !archivezoneerror) {
							if(!dropzoneerror) {
								me.prodropzonepop();
								$scope.$apply();
							}
							if(!archivezoneerror) {
								me.proarchivezonepop();
								$scope.$apply();
							}
						} else {
							me.propop();
							$scope.$apply();
						}
					} else {
						me.sameloadpop();
						$scope.$apply();
					}
					//me.getproperties(false);
				},
				error: function (result) {
					me.errorpop();
					$scope.$apply();
				}
			});
		};

		//$scope.$on('timer-tick', function (event, args) {
			//me.autorefresh();
		//});
		
		$scope.$watch('files', function() {
			$scope.currentUploadFileIndex = 0;
			for (var i = 0; i < $scope.files.length; i++) {
				var file = $scope.files[i];
				$scope.upload = $upload.upload({
					url: 'fileUpload',
					method: 'POST',
					data: {myObj: $scope.myModelObj},
					file: file
				}).progress(function(evt) {
//					console.log('progress: ' + parseInt(100.0 * evt.loaded / evt.total) + '% file :'+ evt.config.file.name);
				}).success(function(data, status, headers, config) {
//					console.log('file ' + config.file.name + 'is uploaded successfully. Response: ' + data);
					var url='?page=custompage_bardigester&action=uploadbar&file=' + data;
					url = url + '&name=' + $scope.files[$scope.currentUploadFileIndex].name;
					$scope.currentUploadFileIndex = $scope.currentUploadFileIndex + 1;
					$.ajax({
						method : 'GET',
						url : url,
						contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
						success : function (result) {
							var resultArray = JSON.parse(result)
							var arrayLength = resultArray.length;
							for (var i = 0; i < arrayLength; i++) {
								$rootScope.history.unshift(resultArray[i]);
								if(resultArray[i].status.toString().startsWith("Error: ")) {
									me.uploadwarningpop();
								} else {
									me.uploadsuccesspop();
								}
							}
							$scope.$apply();
						},
						error: function ( result ) {
							var resultArray = JSON.parse(result)
							var arrayLength = resultArray.length;
							for (var i = 0; i < arrayLength; i++) {
								$rootScope.history.unshift(resultArray[i]);
							}
							me.uploaderrorpop();
							$scope.$apply();
						}
					});
				});
			}
		});
	});
})();
