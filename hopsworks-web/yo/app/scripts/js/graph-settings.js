// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 3000; }

var _getUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };

var vizopsExecutorGraphOptions = function() {
    return {
        chart: {
            type: 'lineChart',
            height: 450,
            margin : {
                top: 20,
                right: 20,
                bottom: 40,
                left: 55
            },
            x: function(d){ return d.x; },
            y: function(d){ return d.y; },
            useInteractiveGuideline: true,
            dispatch: {
                stateChange: function(e){ console.log("stateChange"); },
                changeState: function(e){ console.log("changeState"); },
                tooltipShow: function(e){ console.log("tooltipShow"); },
                tooltipHide: function(e){ console.log("tooltipHide"); }
            },
            xAxis: {
                axisLabel: 'Executors'
            },
            yAxis: {
                axisLabel: 'Voltage (v)',
                tickFormat: function(d){
                    return d3.format('.02f')(d);
                },
                axisLabelDistance: -10
            },
            duration: 500,
            interpolate: 'linear',
            callback: function(chart){
                console.log("!!! lineChart callback !!!");
            }
        },
        title: {
            enable: true,
            text: 'Executor statistics' + _getUpdateLabel()
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