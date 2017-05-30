// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 2000; }
var vizopsGetUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };
var _getColor = ['#9a92e8','#d60606' ,'#025ced','#c67a0f', '#2166ac', '#b2182b', '#c60f0f',
                 '#35978f','#01665e','#39d81a','#40004b', '#d6604d',
                 '#9970ab','#c310d3','#5aae61','#1505aa'];

// OVERVIEW: Total active tasks running in the application
var vizopsTotalActiveTasksOptions = function() {
    return {
        chart: {
            "type": "lineChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
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
              "axisLabel": "Active tasks",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format("d")(d); // TODO: update to d3.format("s") 10000 -> 10K etc
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Total Active Tasks per 30s'
        }
    };
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
    return {
        chart: {
            "type": "multiChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 50,
                "bottom": 60,
                "left": 80
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
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
            "yAxis1": {
              "axisLabel": "Task completion rate",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format("d")(d);
              }
            },
            "y2Axis2": {
              "axisLabel": "Completed tasks",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format("d")(d);
              }
            }
        },
        title: {
            enable: true,
            text: 'Completed Tasks overall'
        }
    };
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

// DRIVER: Heap used
var vizopsMemorySpaceDriverOptions = function() {
    return {
        chart: {
            "type": "lineChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
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
              "axisLabel": "Heap used",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format(".2s")(d);
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Heap used (groupby 20s)'
        }
    };
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
    return {
        chart: {
            "type": "lineChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
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
              "axisLabel": "VCPU usage %",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format(".1%")(d);
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'VCPU usage (groupby 20s)'
        }
    };
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

// EXECUTOR CPU SETUP
var vizopsExecutorCPUOptions = function() {
    return {
        chart: {
            "type": "lineChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
                "bottom": 60,
                "left": 80
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
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
              "axisLabel": "VCores usage %",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format(".1%")(d);
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Aggregated executor VCPU usage'
        }
    };
};

var vizopsExecutorCPUDataTemplate = function() {
    return template = [
       {
           values: [],
           key: 'vcpu',
           color: _getColor[5]
       }
    ];
};

// HOST CPU SETUP
var vizopsHostCPUOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
                "bottom": 60,
                "left": 80
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
            "duration": 500,
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
              "axisLabel": "Core usage",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format(".1f")(d) + "%";
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Host CPU usage'
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

var vizopsHostCPUDataTemplate = function(hostnames) {
    var template = [];

    hostnames.map(function(host) {
        template.push({ values: [], key: host, color: _getColor[1] });
    });

    return template;
};

// Executor MEMORY SETUP
var vizopsExecutorMemoryOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 350,
            "margin": {
                "top": 20,
                "right": 30,
                "bottom": 60,
                "left": 80
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
            "duration": 500,
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
              "axisLabel": "Memory",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                var prefix = d3.formatPrefix(d),
                    format = d3.format(".0f");
                return format(prefix.scale(d)) + prefix.symbol + 'B';
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Executor Memory usage'
        }
    };
};

var vizopsExecutorMemoryDataTemplate = function(nbExecutors) {
    var template = [
           {
               values: [],
               key: 'driver',
               color: _getColor[1]
           }
        ];

    // ignore the driver
    for(var i = 1; i < nbExecutors; i++) {
        template.push({
            values: [],
            key: 'executor_' + i,
            color: _getColor[i % 20]
        });
    }

    return template;
};

// Tasks per host setup
var vizopsTaskPerHostOptions = function() {
    return {
        chart: {
            "height": 350,
            "margin": {
              "top": 30,
              "right": 60,
              "bottom": 50,
              "left": 70
            },
            "duration": 500,

        },
        title: {
            enable: true,
            text: 'Task distribution per node'
        }
    };
};

var vizopsTasksPerHostTemplate = function() { };

// Executors on each node setup
var vizopsExecutorsPerNodeOptions = function() {
    return {
            chart: {
                "type": "pieChart",
                "margin": {
                    "top": 20,
                    "right": 30,
                    "bottom": 60,
                    "left": 80
                },
                "height": 150,
                "showLegend": false,
                "duration": 500,
                "labelThreshold": 0.01,
                "showLabels": false,
                "labelSunbeamLayout": true,
                "x": function(d){return d.key;},
                "y": function(d){return d.y;},
                "legend": {
                  "margin": {
                    "top": 5,
                    "right": 35,
                    "bottom": 5,
                    "left": 0
                  }
                }
            },
            title: {
                enable: true,
                text: 'Executor distribution per node'
            }
        };
};

var vizopsExecutorsPerNodeTemplate = function(hostnames) {
    var template = [];
    for (var key in hostnames) {
        template.push({
            'key': key,
            'y': hostnames[key].length,
            'color': _getColor[1]
        });
    }

    return template;
};
