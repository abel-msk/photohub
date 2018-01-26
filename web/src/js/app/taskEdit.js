
/*


        Наследует  class  taskAdd

 */

define(["jquery","api","modalDialog","logger","schedule","taskAdd"],
    function($,Api,Dialog,logger,Schedule,TaskAdd) {


        "use strict";
        var ADD_TASK_DIALOG_ID = "add_task_dialog";
        var TASK_SELECTOR_GRP_ID = "tsel-grp";
        var TASK_SCHED_FORM_ID = "tsched-form";
        var SAVE_BTN_ID = "save_new_task_btn";

        //------------------------------------------------------------------------
        //------------------------------------------------------------------------

        function TaskEdit() {
            this.task = null;
            //logger.debug ("[TaskEdit.constructor] Start");
        }

        TaskEdit.prototype = Object.create(TaskAdd.prototype);
        TaskEdit.prototype.constructor = TaskAdd;

        //------------------------------------------------------------------------

        //------------------------------------------------------------------------
        TaskEdit.prototype.open = function(task) {
            this.schedule = '';
            this.task = task;
            //TaskAdd.prototype.open(task.siteId);

            $('#'+SAVE_BTN_ID)
                .off('click')
                .on('click',{caller:this},function(event){
                    event.data.caller.save();
                });

            this.siteId = task.siteId;
            this.render(task);

            $("#"+ADD_TASK_DIALOG_ID).modal({'backdrop': true});

        };


        //------------------------------------------------------------------------
        //
        //------------------------------------------------------------------------
        TaskEdit.prototype.render = function(task) {

            //this.taskList = {};

            $('#'+TASK_SELECTOR_GRP_ID + " select").hide();
            $('#'+TASK_SELECTOR_GRP_ID + " div.input-replce")
                .show()
                .text(task.displayName + " | " + task.description );

            this.schedule = this.renderSchedule(task.schedule);

        };

        //------------------------------------------------------------------------
        //
        //         "schedule" : {
        //             "id" : "3",
        //             "taskName" : "TNAME_EMPTY",
        //             "params" : [ {
        //                 "id" : "3",
        //                 "name" : "PARAM1",
        //                 "value" : "theVALUE",
        //                 "type" : null
        //             } ],
        //             "seconds" : "*/5",
        //             "minute" : "*",
        //             "hour" : "*",
        //             "dayOfMonth" : "*",
        //             "month" : "*",
        //             "dayOfWeek" : "*",
        //             "enable" : true
        //             },
        //------------------------------------------------------------------------
        TaskEdit.prototype.save = function() {
            this.schedule = this.scheduleObj.getCron(this.schedule);
            this.schedule.params = this.getParams();
            var task = this.task;

            //  Send Schedule object to backend
            Api.editTask(this.siteId,this.schedule,
                //  on Success
                function(response) {
                    try {
                        if ( response.object && (response.rc == 0)) {
                            task.schedule = response.object;
                            $('body').trigger('task:edit', [task]);
                            logger.debug("[taskEdit.save] Object Saved",task );
                        }
                    }
                    catch (e) {
                        logger.debug("[taskEdit.save] Error: ",e);
                    }
                },

                //  on Error
                function(response){
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
            this.close();
        };



        return TaskEdit;
    });