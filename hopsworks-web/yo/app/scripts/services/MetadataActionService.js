/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .service('MetadataActionService', function (WSComm) {

          return {
            fetchTemplates: function (user) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'fetch_templates',
                message: JSON.stringify({})
              });
            },
            addNewTemplate: function (user, templatename) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'add_new_template',
                message: JSON.stringify({templateName: templatename})
              });
            },
            removeTemplate: function (user, templateid) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'remove_template',
                message: JSON.stringify({templateId: templateid})
              });
            },
            updateTemplateName: function (user, template) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'update_template_name',
                message: JSON.stringify({templateId: template.id, templateName: template.name})
              });
            },
            extendTemplate: function (user, templateId, board) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'extend_template',
                message: JSON.stringify({tempid: templateId, bd: board})
              });
            },
            fetchTemplate: function (user, templateId) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'fetch_template',
                message: JSON.stringify({tempid: templateId})
              });
            },
            storeTemplate: function (user, templateId, board) {
              return WSComm.send({
                sender: user,
                type: 'TemplateMessage',
                action: 'store_template',
                message: JSON.stringify({tempid: templateId, bd: board})
              });
            },
            deleteList: function (user, templateId, column) {
              return WSComm.send({
                sender: user,
                type: 'TableMessage',
                action: 'delete_table',
                message: JSON.stringify({
                  tempid: templateId,
                  id: column.id,
                  name: column.name,
                  forceDelete: column.forceDelete
                })
              });
            },
            storeCard: function (user, templateId, column, card) {
              return WSComm.send({
                sender: user,
                type: 'FieldMessage',
                action: 'store_field',
                message: JSON.stringify({
                  tempid: templateId,
                  tableid: column.id,
                  tablename: column.name,
                  id: card.id,
                  name: card.title,
                  type: 'VARCHAR(50)',
                  searchable: card.find,
                  required: card.required,
                  sizefield: card.sizefield,
                  description: card.description,
                  fieldtypeid: card.fieldtypeid,
                  fieldtypeContent: card.fieldtypeContent,
                  position: card.position
                })
              });
            },
            deleteCard: function (user, templateId, column, card) {
              return WSComm.send({
                sender: user,
                type: 'FieldMessage',
                action: 'delete_field',
                message: JSON.stringify({
                  tempid: templateId,
                  id: card.id,
                  tableid: column.id,
                  tablename: column.name,
                  name: card.title,
                  type: 'VARCHAR(50)',
                  sizefield: card.sizefield,
                  searchable: card.find,
                  required: card.required,
                  forceDelete: card.forceDelete,
                  description: card.description,
                  fieldtypeid: card.fieldtypeid,
                  fieldtypeContent: card.fieldtypeContent,
                  position: card.position
                })
              });
            },
            fetchFieldTypes: function (user) {
              return WSComm.send({
                sender: user,
                type: 'FieldTypeMessage',
                action: 'fetch_field_types',
                message: 'null'
              });
            },
            fetchMetadata: function (user, tableId, inodePid, inodeName) {
              return WSComm.send({
                sender: user,
                type: 'FetchMetadataMessage',
                action: 'fetch_metadata',
                message: JSON.stringify({
                  tableid: tableId, 
                  inodepid: inodePid,
                  inodename: inodeName
                })
              });
            },
            fetchTableMetadata: function (user, tableId) {
              return WSComm.send({
                sender: user,
                type: 'FetchTableMetadataMessage',
                action: 'fetch_table_metadata',
                message: JSON.stringify({tableid: tableId})
              });
            },
            //contains an inode mutation message as well (projectid, inodeid)
            storeMetadata: function (user, parentid, inodename, tableid, data) {
              return WSComm.send({
                sender: user,
                type: 'StoreMetadataMessage',
                action: 'store_metadata',
                message: JSON.stringify({
                  inodepid: parentid,
                  inodename: inodename,
                  tableid: tableid,
                  metadata: data
                })
              });
            },         
            isTableEmpty: function (user, tableId) {
              return WSComm.send({
                sender: user,
                type: 'CheckTableContentMessage',
                action: 'is_table_empty',
                message: JSON.stringify({tableid: tableId})
              });
            },
            isFieldEmpty: function (user, fieldId) {
              return WSComm.send({
                sender: user,
                type: 'CheckFieldContentMessage',
                action: 'is_field_empty',
                message: JSON.stringify({fieldid: fieldId})
              });
            },
            updateMetadata: function (user, metaObj, inodepid, inodename) {
              return WSComm.send({
                sender: user,
                type: 'UpdateMetadataMessage',
                action: 'update_metadata',
                message: JSON.stringify({
                  metaid: metaObj.id, 
                  inodepid: inodepid,
                  inodename: inodename,
                  tableid: -1, //table id is not necessary when updating metadata
                  metadata: metaObj.data})
              });
            },
            removeMetadata: function (user, metaObj, inodepid, inodename) {
              return WSComm.send({
                sender: user,
                type: 'RemoveMetadataMessage',
                action: 'remove_metadata',
                message: JSON.stringify({
                  metaid: metaObj.id, 
                  inodepid: inodepid,
                  inodename: inodename,
                  tableid: -1, //table id is not necessary when updating metadata
                  metadata: metaObj.data})
              });
            }
          };
        });