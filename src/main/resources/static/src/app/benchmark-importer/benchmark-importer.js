angular.module('ngBoilerplate.benchmarkImporter',['ui.router', 'ngResource', 'base64'])
.config(function($stateProvider) {
    $stateProvider
   .state('benchmarkImporterManagement', {
            url:'/benchmarkImporterManagement',
            views: {
                'main': {
                    templateUrl:'/src/app/benchmark-importer/benchmark-importer.html',
                    controller: 'BenchmarkImporterCtrl'
                }
            },
            data : { pageTitle : "Benchmark" },
            resolve: {    
                analyzers: function(benchmarkImporters2) {
                 return benchmarkImporters2.getAllBenchMarkImporters();
             },      
             role: function(accountService) {
                 return accountService.getRole();
             },
             IsLoged: function(accountService) {
                return accountService.isUserLoggedIn();
             }
        }
    });
})
.factory('benchmarkImporters2', function($resource) {
    var service = {};
    service.getAllBenchMarkImporters = function() {
     var Benchmark = $resource("/LuceneAnalyzerTester/rest/benchmark-importer/getAll");
        return Benchmark.get().$promise.then(function(data) {
            return data.analyzers;
          });
    };
    return service;
})
.factory('BenchmarkImporterUploadService', ["$resource", function($resource) {
    var baseUrl = '/LuceneAnalyzerTester/rest/benchmark-importer';
    return $resource(baseUrl, {},
        {
            add: {
                url: baseUrl + '/newBenchmarkImporter',
                headers: {
                    'Content-Type': undefined
                },
                transformRequest: angular.identity,
                method: 'POST'
            }
        });
}])

.controller("BenchmarkImporterCtrl", function($scope,$state,$http, BenchmarkImporterUploadService, analyzers ,role,IsLoged) {
    
    $scope.isLoggedIn = IsLoged;
    $scope.role = role;
    $scope.analyzers = analyzers;
    $scope.addMode = false;
    $scope.selectedAnalyzer = null;
    $scope.message = ""; 
    
    $scope.selectedRow = null;
    $scope.setClickedRow = function (index, analyzer) {  //function that sets the value of selectedRow to current index
         if (index == $scope.selectedRow) {           
             $scope.selectedRow = null;
             $scope.selectedAnalyzer = null;
         }
         else {
			$scope.selectedRow = index; 
			$scope.selectedAnalyzer = analyzer; 
			$scope.message = "";     
			$scope.addMode = false;
         }
     };
    
    $scope.uploadFile=function(){
      var formData = new FormData();
      formData.append('formDataJson', JSON.stringify($scope.touristObject));  
      formData.append('file', file.files[0]);
      formData.append('name', $scope.analyzer.name);
      formData.append('description', $scope.analyzer.description);
      
            BenchmarkImporterUploadService.add(formData).$promise.then(function () {
                $http.get("/LuceneAnalyzerTester/rest/benchmark-importer/getAll").success(function(data, status) {
                   $scope.analyzers = data.analyzers;              
                }).error(function(data, status) {
                 alert("Error ... " + status);
                });                   
            }, function () {
                alert('Error while uploading benchmark importer!');
            });
   };
  
   $scope.removeAnalyzer = function() {
    if($scope.selectedAnalyzer !== null) {
       $scope.message = "";
       $http.post('/LuceneAnalyzerTester/rest/benchmark-importer/remove', $scope.selectedAnalyzer.id).success(function(data, status) {                     
                $http.get('/LuceneAnalyzerTester/rest/benchmark-importer/getAll').success(function(data, status) { 
                     $scope.analyzers = data.analyzers;
                     $scope.selectedAnalyzer = null;
                }).error(function(data, status) {
                    alert("Error ... " + status);
                });
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
            
     }
     else {
         $scope.message = "Please select analyzer from the list first!";
     }
   };
  
   $scope.changeToAddMode = function() {
        $scope.addMode = true;
        $scope.message = "";
   };
  
   $scope.backFromAddMode = function() {
        $scope.addMode = false;
   };
  
   $scope.logout = function() {   
		$scope.isLoggedIn = false;
		$http.post("/logout")
        .success(function () {  
           $state.go("home");         
        })
        .error(function (data) {
        });    
    };
});
