/**
 * Created by abel on 03.11.15.
 */

define([],function(){

    var loggerClass =  {
        'debug':function() {
            if (typeof arguments == "string")  {
                console.log(arguments);
            }
            else {
                for (var i = 0; i < arguments.length; i++) {
                    console.log(arguments[i]);
                }
            }
        },

        'trace': function() {
            if (typeof arguments == "string")  {
                console.log(arguments);
            }
            else {

                for (var i = 0; i < arguments.length; i++) {
                    console.log("   > " + arguments[i]);
                }

                ////var e = new Error('dummy');
                //console.log(arguments[0]);
                //var stack = arguments[1].replace(/^[^\(]+?[\n$]/gm, '')
                //    .replace(/^\s+at\s+/gm, '')
                //    .replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@')
                //    .split('\n');
                //console.log(stack);
                //
                //
                //console.log("------------");
                //
                //var e = new Error('dummy');
                //var stack = e.stack.replace(/^[^\(]+?[\n$]/gm, '')
                //    .replace(/^\s+at\s+/gm, '')
                //    .replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@')
                //    .split('\n');
                //console.log(stack);
                //
                //console.log(e.stack);

            }
        }
    };

    return loggerClass;
});