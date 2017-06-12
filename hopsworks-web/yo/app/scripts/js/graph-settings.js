// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 2000; }
var vizopsGetUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };
var _getColor = ['#9a92e8','#d60606' ,'#025ced','#c67a0f', '#2166ac', '#b2182b', '#c60f0f',
                 '#35978f','#01665e','#39d81a','#40004b', '#d6604d',
                 '#9970ab','#c310d3','#5aae61','#1505aa'];

var getBaseChartOptions = function() {
    return {
        chart: {
            "type": "lineChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 80,
                "bottom": 60,
                "left": 80
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
            "forceY": [0],
            "duration": 500,
            "showLegend": false,
            "useInteractiveGuideline": true,
            "xAxis": {
              "axisLabel": "Time",
              "rotateLabels": -35,
              "tickFormat": function(d) {
                return d3.time.format("%H:%M:%S")(new Date(d));
              }
            },
            "x2Axis": {
              "tickFormat": function(d) {
                return d3.time.format("%H:%M:%S")(new Date(d));
              }
            },
            "yAxis": {
              "axisLabel": "Default",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format("d")(d);
              }
            },
            "yAxis1": {},
            "yAxis2": {},
            "y2Axis": {},

        },
        title: {
            enable: true,
            text: 'Default title'
        },
        subtitle: {
            enable: false,
            text: 'Updates every ' + (vizopsUpdateInterval()/1000) + ' s',
            css: {
                'text-align': 'center',
                'margin': '10px 13px 0px 7px'
            }
        },
        caption: {
            enable: false,
            html: 'Test graph retrieving live updates',
            css: {
                'text-align': 'justify',
                'margin': '10px 13px 0px 7px'
            }
        }
    };
};

// OVERVIEW: Total active tasks running in the application
var vizopsTotalActiveTasksOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Tasks",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format("d")(d); // TODO: update to d3.format("s") 10000 -> 10K etc
        }
      };
    options.title.text = 'Total Active Tasks per 30s';

    return options;
};

var vizopsTotalActiveTasksTemplate = function() {
    return [
        {
            values: [],
            key: 'active tasks',
            color: _getColor[1]
        }
    ];
};

// OVERVIEW: Total completed tasks in the application
var vizopsTotalCompletedTasksOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiChart';
    options.chart.yAxis1 = {
         "axisLabel": "Task completion rate",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format("d")(d);
         }
       };
    options.chart.yAxis2 ={
        "axisLabel": "Completed tasks",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      } ;
    options.title.text = 'Completed Tasks overall';

    return options;
};

var vizopsTotalCompletedTasksTemplate = function() {
    return [
        {
            values: [],
            key: 'rate of completion',
            color: _getColor[1],
            type: "line",
            yAxis: 1
        },
        {
            values: [],
            key: 'completed tasks',
            color: _getColor[2],
            type: "line",
            yAxis: 2
        }
    ];
};

// OVERVIEW: HDFS read rate and total
var vizopsHDFSReadRateTotalOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS read bytes overall';

    return options;
};

var vizopsHDFSReadRateTotalTemplate = function() {
    return [
        {
            values: [],
            key: 'rate read bytes',
            color: _getColor[6]
        },
        {
            values: [],
            key: 'total read bytes',
            color: _getColor[10]
        }
    ];
};

// OVERVIEW: HDFS write rate and total
var vizopsHDFSWriteRateTotalOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS write bytes overall';

    return options;
};

var vizopsHDFSWriteRateTotalTemplate = function() {
    return [
        {
            values: [],
            key: 'rate write bytes',
            color: _getColor[6]
        },
        {
            values: [],
            key: 'total write bytes',
            color: _getColor[10]
        }
    ];
};

// DRIVER: Heap used
var vizopsMemorySpaceDriverOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format(".2s")(d);
        }
      };
    options.title.text = 'Heap used (groupby 20s)';

    return options;
};

var vizopsMemorySpaceDriverTemplate = function() {
    return [
        {
            values: [],
            key: 'avg',
            color: _getColor[2]
        },
        {
            values: [],
            key: 'max',
            color: _getColor[14]
        }
    ];
};

// DRIVER: VCPU used
var vizopsVCPUDriverOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "VCPU usage %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1%")(d);
        }
      };
    options.title.text = 'VCPU usage (groupby 20s)';

    return options;
};

var vizopsVCPUDriverTemplate = function() {
    return [
        {
            values: [],
            key: 'vcpu',
            color: _getColor[10]
        }
    ];
};

// DRIVER: RDD CACHE Block Manager
var vizopsRDDCacheDiskSpillOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = "multiChart";
    options.chart.yAxis1 = {
         "axisLabel": "RDD Cache Bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.chart.yAxis2 ={
        "axisLabel": "Disk spill Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      } ;
    options.title.text = "BlockManager";

    return options;
};

var vizopsRDDCacheDiskSpillTemplate = function() {
    return [
        {
            values: [],
            key: 'rdd cache',
            color: _getColor[3],
            type: "line",
            yAxis: 1
        },
        {
            values: [],
            key: 'disk spill',
            color: _getColor[5],
            type: "line",
            yAxis: 2
        }
    ];
};

// DRIVER: GC TIME
var vizopsGCTimeOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "GC Time",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
      };
    options.title.text = 'GC Time';

    return options;
};

var vizopsGCTimeTemplate = function() {
    return [
        {
            values: [],
            key: 'Mark&Sweep',
            color: _getColor[11]
        },
        {
            values: [],
            key: 'Scavenge',
            color: _getColor[8]
        }
    ];
};

// EXECUTOR: HDFS/DISK READ
var vizopsExecutorHDFSDiskReadOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiChart';
    options.chart.yAxis1 = {
         "axisLabel": "HDFS Read bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.chart.yAxis2 = {
        "axisLabel": "Disk Read bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS/Disk read';

    return options;
};

var vizopsExecutorHDFSDiskReadTemplate = function() {
    return [
       {
           values: [],
           key: 'HDFS Read',
           color: _getColor[7],
           type: "line",
           yAxis: 1
       },
       {
           values: [],
           key: 'Disk Read',
           color: _getColor[14],
           type: "line",
           yAxis: 2
       }
    ];
};

// EXECUTOR: HDFS/DISK WRITE
var vizopsExecutorHDFSDiskWriteOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiChart';
    options.chart.yAxis1 = {
         "axisLabel": "HDFS Write bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.chart.yAxis2 = {
        "axisLabel": "Disk Write bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS/Disk write';

    return options;
};

var vizopsExecutorHDFSDiskWriteTemplate = function() {
    return [
       {
           values: [],
           key: 'HDFS Write',
           color: _getColor[7],
           type: "line",
           yAxis: 1
       },
       {
           values: [],
           key: 'Disk Write',
           color: _getColor[14],
           type: "line",
           yAxis: 2
       }
    ];
};

// EXECUTOR: GC TIME
var vizopsExecutorGCTimeOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "GC Time",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
      };
    options.title.text = 'GC Time';

    return options;
};

var vizopsExecutorGCTimeTemplate = function() {
    return [
       {
           values: [],
           key: 'Mark&Sweep',
           color: _getColor[11]
       },
       {
           values: [],
           key: 'Scavenge',
           color: _getColor[8]
       }
    ];
};

// EXECUTOR CPU
var vizopsExecutorCPUOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "VCPU usage %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1%")(d);
        }
      };
    options.title.text = 'Executor VCPU usage';

    return options;
};

var vizopsExecutorCPUDataTemplate = function() {
    return [
       {
           values: [],
           key: 'vcpu',
           color: _getColor[5]
       }
    ];
};

// EXECUTOR MEMORY
var vizopsExecutorMemoryUsageOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'lineChart';
    options.chart.yAxis = {
       "axisLabel": "Average memory",
       "rotateYLabel": true,
       "tickFormat": function(d) {
        return d3.format(".2s")(d);
      }
    };
    options.title.text = 'Executor memory';

    return options;
};

var vizopsExecutorMemoryUsageTemplate = function() {
    return [
       {
           values: [],
           key: 'mean',
           color: _getColor[10]
       },
       {
           values: [],
           key: 'threshold',
           color: _getColor[1]
       }
    ];
};

// EXECUTOR TASK DISTRIBUTION
var vizopsExecutorTaskDistributionOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Executors",
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Tasks",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format("d")(d);
      }
    };
    options.title.text = 'Task distribution';

    return options;
};

var vizopsExecutorTaskDistributionTemplate = function() {
    return [
        {
            key: 'Tasks',
            values: []
        }
    ];
};

// EXECUTOR Peak memory per executor
var vizopsExecutorPeakMemoryOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Executors",
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Peak memory",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format(".2s")(d);
      }
    };
    options.title.text = 'Peak memory';

    return options;
};

var vizopsExecutorPeakMemoryTemplate = function() {
    return [
        {
            key: 'Peak memory per executor',
            values: [],
            color: _getColor[5]
        }
    ];
};

// WORKER Physical cpu
var vizopsWorkerPhysicalCpuOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Physical CPU %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1f")(d) + '%';
        }
      };
    options.title.text = 'Physical CPU average';

    return options;
};

var vizopsWorkerPhysicalCpuTemplate = function() {
    return [
        {
            values: [],
            key: 'usage',
            color: _getColor[10]
        },
        {
            values: [],
            key: 'iowait',
            color: _getColor[12]
        },
        {
            values: [],
            key: 'idle',
            color: _getColor[13]
        },

    ];
};

// WORKER Memory
var vizopsWorkerMemoryUsageOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
         "axisLabel": "Bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.title.text = 'Memory usage';

    return options;
};

var vizopsWorkerMemoryUsageTemplate = function() {
    return [
        {
            values: [],
            key: 'used',
            color: _getColor[1]
        },
        {
            values: [],
            key: 'available',
            color: _getColor[3]
        }
    ];
};

// WORKER Network
var vizopsWorkerNetworkTrafficOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Bytes rate of change",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'Network usage(rate of change)';

    return options;
};

var vizopsWorkerNetworkTrafficTemplate = function() {
     return [
         {
             values: [],
             key: 'received',
             color: _getColor[10]
         },
         {
             values: [],
             key: 'sent',
             color: _getColor[10]
         }
     ];
 };

// Containers/host
// Executors/host
// List of down hosts
// OVERVIEW io cpu bottleneck
