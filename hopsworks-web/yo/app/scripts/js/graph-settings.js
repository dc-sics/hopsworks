// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 2000; }
var _getUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };
var _getColor = d3.range(20).map(d3.scale.category20());

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
                "height": 300,
                "duration": 500,
                "labelThreshold": 0.01,
                "showLabels": true,
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

var vizopsExecutorsPerNodeTemplate = function() {
    /* Pie chart data look like this:
     [
        {
            key: "One",
            y: 5
        },
        {
            key: "Two",
            y: 2
        }
    ]
    Key: hostname
    y: executors running on that host
    */
    var template = [

    ];

    return template;
};
