// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 2000; }
var vizopsGetUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };
var _getColor = ['#8c510a','#bf812d', '#2166ac', '#b2182b', '#543005',
                 '#35978f','#01665e','#003c30','#40004b', '#d6604d', '#762a83',
                 '#9970ab','#c2a5cf','#5aae61','#1b7837','#00441b'];

var vizopsMapColorExecutorsHost = function(hostnames) {
    var result = {}, i = 0;

    for (var key in hostnames) {
        if (hostnames.hasOwnProperty(key)) {
            result[key] = _getColor[i % 20];
            for (var executor of hostnames[key]) {
                result[executor] = _getColor[i % 20];
            }
        }
        i++;
    }

    return result;
};

// EXECUTOR CPU SETUP
var vizopsExecutorCPUOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 450,
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
              "axisLabel": "VCores usage",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format(".1%")(d);
              }
            },
            "y2Axis": {}
        },
        title: {
            enable: true,
            text: 'Executor VCPU usage'
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

var vizopsExecutorCPUDataTemplate = function(nbExecutors, colorMap) {
    var template = [
       {
           values: [],
           key: 'driver',
           color: colorMap['0']
       }
    ];

    // ignore the driver
    for(var i = 1; i < nbExecutors; i++) {
        template.push({
            values: [],
            key: 'executor_' + i,
            color: colorMap['' + i]
        });
    }

    return template;
};

// HOST CPU SETUP
var vizopsHostCPUOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 450,
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

var vizopsHostCPUDataTemplate = function(hostnames, colorMap) {
    var template = [];

    hostnames.map(function(host) {
        template.push({ values: [], key: host, color: colorMap[host] });
    });

    return template;
};

// Executor MEMORY SETUP
var vizopsExecutorMemoryOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 450,
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

var vizopsExecutorMemoryDataTemplate = function(nbExecutors, colorMap) {
    var template = [
           {
               values: [],
               key: 'driver',
               color: colorMap['0']
           }
        ];

    // ignore the driver
    for(var i = 1; i < nbExecutors; i++) {
        template.push({
            values: [],
            key: 'executor_' + i,
            color: colorMap['' + i]
        });
    }

    return template;
};

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
                "height": 200,
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

/*
    @arg(hostnames): a dictionary with keys hostnames and a list of executors running on them
*/
var vizopsExecutorsPerNodeTemplate = function(hostnames, colorMap) {
    var template = [];
    for (var key in hostnames) {
        template.push({
            'key': key,
            'y': hostnames[key].length,
            'color': colorMap[key]
        });
    }

    return template;
};
