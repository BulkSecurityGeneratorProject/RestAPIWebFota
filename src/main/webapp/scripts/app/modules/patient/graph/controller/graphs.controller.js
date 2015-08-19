'use strict';

angular.module('hillromvestApp')
.controller('graphController', function($scope, $state, patientDashBoardService, StorageService, dateService, graphUtil) {
    var chart;
    $scope.init = function() {
      $scope.complianceToggle = false;
      $scope.hmrLineGraph = true;
      $scope.hmrBarGraph = false;
      $scope.hmrGraph = true;
      $scope.format = 'monthly';
      $scope.compliance = {};
      $scope.compliance.pressure = true;
      $scope.compliance.duration = true;
      $scope.compliance.frequency = false;
      $scope.fromTimeStamp = dateService.getnDaysBackTimeStamp();
      $scope.toTimeStamp = Date.now();
      $scope.handlelegends();
      $scope.groupBy = 'weekly'
      if ($state.current.name === 'patientdashboard') {
        $scope.weeklyChart();
      }
      $scope.patientId = StorageService.get('patientID') || 5 ;

    };

    $scope.xAxisTickFormatFunction = function(format){
      return function(d){
        switch(format) {
          case "weekly":
              return d3.time.format('%a')(new Date(d));
              break;
          case "dayWise":
              return dateService.getTimeIntervalFromTimeStamp(d);
              break;
          case "monthly":
              return 'week ' + dateService.getWeekOfMonth(d);
              break;
          case "yearly":
              return d3.time.format('%B')(new Date(d));
              break;
          default:
              break;
        }
    }
  }

    $scope.toolTipContentFunction = function(data){
      return function(key, x, y, e, graph) {
        var toolTip = '';
        angular.forEach(data, function(value) {
          if(value.timeStamp === e.point[0]){
              toolTip =
                '<h6>' + dateService.getDateFromTimeStamp(value.timeStamp) + '</h6>' +
                '<p> Treatment/Day ' + value.treatmentsPerDay + '</p>' +
                '<p> Frequency ' + value.weightedAvgFrequency + '</p>' +
                '<p> Pressure ' + value.weightedAvgPressure + '</p>' +
                '<p> Caugh Pauses ' + value.normalCaughPauses + '</p>';
          }
        });
      return toolTip;   
      }
    }

    $scope.toolTipContentForCompliance = function(data){
      return function(key, x, y, e, graph) {
        var toolTip = '';
        angular.forEach(data, function(value) {
          if(value.date === e.point.x){
              toolTip =
                '<h6>' + dateService.getDateFromTimeStamp(value.date) + '</h6>' +
                '<p> Treatment/Day ' + value.therapyData.treatmentsPerDay + '</p>' +
                '<p> Frequency ' + value.therapyData.weightedAvgFrequency + '</p>' +
                '<p> Pressure ' + value.therapyData.weightedAvgPressure + '</p>' +
                '<p> Caugh Pauses ' + value.therapyData.normalCaughPauses + '</p>';
          }
        });
      return toolTip;   
      }
    }

    $scope.xAxisTickValuesFunction = function(){
    return function(d){
        var tickVals = [];
        var values = d[0].values;
        for(var i in values){
          tickVals.push(values[i][0]);
        }
        console.log('xAxisTickValuesFunction', d);
        return tickVals;
      };
    };

   /* $scope.$on('elementClick.directive', function(angularEvent, event) {
      console.log(event);
      $scope.createGraphData();
      $scope.$digest();
    });*/
    $scope.showHmrGraph = function() {
      $scope.complianceGraph = false;
      $scope.hmrGraph = true;
    };

    $scope.getNonDayHMRGraphData = function() {
      $scope.completeGraphData = HMRWeeklyGraphData;
      $scope.graphData = graphUtil.convertIntoHMRLineGraph($scope.completeGraphData);
      patientDashBoardService.getHMRGraphPoints($scope.patientId, $scope.fromTimeStamp, $scope.toTimeStamp, $scope.groupBy).then(function(response){
        //Will get response data from real time API once api is ready
      }).catch(function(response) {});
    };

    $scope.getDayHMRGraphData = function() {
      $scope.completeGraphData = HMRDayGraphData;
      $scope.graphData = graphUtil.convertIntoHMRBarGraph($scope.completeGraphData);
      patientDashBoardService.getHMRGraphPoints($scope.patientId, $scope.timeStamp).then(function(response){
        //Will get response data from real time API once api is ready
      }).catch(function(response) {});
    };

    $scope.getComplianceGraphData = function() {
      $scope.completeComplianceData = complianceGraphData;
      $scope.completecomplianceGraphData = graphUtil.convertIntoComplianceGraph($scope.completeComplianceData);
     /* patientDashBoardService.getComplianceGraphPoints($scope.patientId, $scope.fromTimeStamp, $scope.toTimeStamp, $scope.groupBy).then(function(response){
        //Will get response data from real time API once api is ready
      }).catch(function(response) {});*/
    };

    // Weekly chart
    $scope.weeklyChart = function() {
      $scope.format = 'weekly';
      if($scope.hmrGraph) {
        $scope.hmrLineGraph = true;
        $scope.hmrBarGraph = false;
        $scope.getNonDayHMRGraphData();
      } else if ($scope.complianceGraph) {
        $scope.getComplianceGraphData();
        $scope.createComplianceGraphData();
        $scope.drawComplianceGraph();
      }
    }
    // Yearly chart
    $scope.yearlyChart = function() {
       $scope.format = 'yearly';
        if($scope.hmrGraph) {
          $scope.hmrLineGraph = true;
          $scope.hmrBarGraph = false;
          $scope.getNonDayHMRGraphData();
      } else if ($scope.complianceGraph) {
          $scope.getComplianceGraphData();
          $scope.createComplianceGraphData();
          $scope.drawComplianceGraph();
      }
    }
   
    // Monthly chart
    $scope.monthlyChart = function() {
      $scope.format = 'monthly';
      if($scope.hmrGraph) {
        $scope.hmrLineGraph = true;
        $scope.hmrBarGraph = false;
        $scope.getNonDayHMRGraphData();
      } else if ($scope.complianceGraph) {
        $scope.getComplianceGraphData();
        $scope.createComplianceGraphData();
        $scope.drawComplianceGraph();
      }
    }
    //hmrDayChart
    $scope.dayChart = function() {
       if($scope.hmrGraph) {
        $scope.format = 'dayWise';
        $scope.hmrLineGraph = false;
        $scope.hmrBarGraph = true;
        $scope.getDayHMRGraphData();
      }
    }

    $scope.showComplianceGraph = function() {
      $scope.complianceGraph = true;
      $scope.hmrGraph = false;
      $scope.getComplianceGraphData();
      $scope.createComplianceGraphData();
      $scope.drawComplianceGraph();
  };

  $scope.createComplianceGraphData = function() {
    delete $scope.complianceGraphData ;
    $scope.complianceGraphData = [];
    var count = 1;
    angular.forEach($scope.completecomplianceGraphData, function(value) {
          if(value.key.indexOf("pressure") >= 0 && $scope.compliance.pressure){
            value.yAxis = count++;
            $scope.complianceGraphData.push(value);
          }
          if(value.key.indexOf("duration") >= 0 && $scope.compliance.duration){
            value.yAxis = count++;
            $scope.complianceGraphData.push(value);
          }
          if(value.key.indexOf("frequency") >= 0  && $scope.compliance.frequency){
            value.yAxis = count++;
            $scope.complianceGraphData.push(value);
          }
    });
    console.log('compliance graph data!')
    console.log($scope.complianceGraphData)
  }

  $scope.putComplianceGraphLabel = function(chart) {
    var data =  $scope.complianceGraphData
     angular.forEach(data, function(value) {
          if(value.yAxis === 1){
            chart.yAxis1.axisLabel(value.key);
          }
           if(value.yAxis === 2){
            chart.yAxis2.axisLabel(value.key);
          }
    });
  }
  $scope.handlelegends = function() {
    var count = 0 ;
    if($scope.compliance.pressure === true ){
      count++;
    }
    if($scope.compliance.duration === true ){
      count++;
    }
    if($scope.compliance.frequency === true ){
      count++;
    }
    if(count === 2 ) {
      if($scope.compliance.pressure === false ){
        $scope.pressureIsDisabled = true;
      }
      if($scope.compliance.frequency === false ){
        $scope.frequencyIsDisabled = true;
      }
      if($scope.compliance.duration === false ){
        $scope.durationIsDisabled = true;
      }
    } else if(count < 2 ) {
       $scope.pressureIsDisabled = false;
       $scope.frequencyIsDisabled = false;
       $scope.durationIsDisabled = false;
    }
  }
  $scope.reDrawCompliancegraph = function() {

  };

  $scope.reCreateComplianceGraph = function() {
    $scope.handlelegends();
    $scope.complianceToggle = !$scope.complianceToggle;
    $scope.createComplianceGraphData();
    $scope.drawComplianceGraph();
  };

  $scope.formatXtickForCompliance = function(format,d){
        switch(format) {
          case "weekly":
              return d3.time.format('%a')(new Date(d));
              break;
          case "monthly":
              return 'week ' + dateService.getWeekOfMonth(d);
              break;
          case "yearly":
              return d3.time.format('%B')(new Date(d));
              break;
          default:
              break;
        }
  }

  $scope.drawComplianceGraph = function() {
    d3.select('#complianceGraph svg').selectAll("*").remove();
      nv.addGraph(function() {
      chart = nv.models.multiChart()
      .margin({top: 30, right: 100, bottom: 50, left: 100})
      .color(d3.scale.category10().range());
      chart.tooltipContent($scope.toolTipContentForCompliance($scope.completeComplianceData));
      chart.xAxis.tickFormat(function(d) {
        switch($scope.format) {
          case "weekly":
              return d3.time.format('%a')(new Date(d));
              break;
          case "monthly":
              return 'week ' + dateService.getWeekOfMonth(d);
              break;
          case "yearly":
              return d3.time.format('%B')(new Date(d));
              break;
          default:
              break;
        }
      });
      chart.yAxis1.tickFormat(d3.format(',.0f'));
      chart.yAxis2.tickFormat(d3.format(',.0f'));
      $scope.putComplianceGraphLabel(chart);
        d3.select('#complianceGraph svg')
      .datum($scope.complianceGraphData)
      .transition().duration(500).call(chart);
      return chart;
    });
  }
    $scope.init();
});

