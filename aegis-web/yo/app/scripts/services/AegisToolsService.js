'use strict';

angular.module('hopsWorksApp')
    .factory('AegisToolsService', ['$http', '$q', function ($http, $q) {
        var self = this;

        // generic img: images/jupyter.png
        self.tools = [
            {
                name: "Query Builder",
                notebook: "QueryBuilder_v1.ipynb",
                img: "images/tools-jupyter.png", 
                description: "Design queries, combine and process data in AEGIS with ease and clarity.",
                documentationUrl: "https://github.com/aegisbigdata/documentation/wiki/Query-Builder",
                sourceUrl: "https://github.com/aegisbigdata/query-builder"
            },
            {
                name: "Visualiser",
                notebook: "Visualizer.ipynb",
                img: "images/tools-visualizer.png",
                description: "Easily transform heaps of data to a wide sset of visualisations.",
                documentationUrl: "https://github.com/aegisbigdata/documentation/wiki/Visualizer",
                sourceUrl: "https://github.com/aegisbigdata/visualizer"
            },
            {
                name: "Algorithm Execution Container",
                notebook: "Algorithm Execution Container-Jupyter Version.ipynb",
                img: "images/tools-aec.png",
                description: "Manage your algorithms and execute them in an efficient manner.",
                documentationUrl: "https://github.com/aegisbigdata/documentation/wiki/Algorithm-Execution-Container",
                sourceUrl: "https://github.com/aegisbigdata/AlgorithmContainer"
            }
        ];

        return {
            getAllTools: function (projectId) {
                return $q(function (resolve, reject) {
                    resolve({
                        data:self.tools
                    });
                });
            }
        };
    }]);

