/**
 * Created by ksinger on 17.06.2016.
 */

;

import angular from 'angular';
import 'angular-sanitize';
import { attach, delay } from '../utils';

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <div class="wdt__text valid">
                <div class="medium-mount"></div>
            </div>
        </div>
    `;

    const serviceDependencies = ['$scope', '$element', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ntf4dbg = this;

            this.initMedium();

            this.mediumMountNg().bind('input', e => this.input(e));
            this.mediumMountNg().bind('paste', e => this.input(e));
        }
        input(e) {
            console.log("input!");
            const paragraphsDom = this.$element.find('p').toArray();
            const paragraphsNg = paragraphsDom.map(p => angular.element(p)); // how performant is `.element`?
            paragraphsNg.map(p => p.removeClass("medium_title"));

            const titleParagraphDom = paragraphsDom[0];
            const titleParagraphNg = paragraphsNg[0];
            titleParagraphNg.addClass("medium_title");

            var description;
            var tags;
            if(paragraphsDom && paragraphsDom.length > 1){
                const descriptionParagraphs = paragraphsNg.slice(1);
                description = descriptionParagraphs.map(p =>
                        p.text()
                          /* remove trailing white-spaces (e.g. bogus-line-breaks,
                           * i.e. the ones that aren't <p>)
                           */
                          .replace(/\s*$/,'')
                    )
                    .join('\n') // concatenate lines
                    /*
                     * remove leading/trailing empty lines that occur between title
                     * and description when pasting multi-line text
                    */
                    .trim();
            } else {
                description = undefined;
            }
            this.draft.description = description;
            console.log("draft-description: ",this.draft.description);

            /*
             * Remove placeholder-white-space if medium.js fails to remove it,
             * e.g. when pasting (a multi-line'd string) into an empty textfield.
             * The `[0]` access the dom-element inside of the angular-element.
             */
            //titleParagraphDom.innerHTML = titleParagraphDom.innerHTML.replace(/^&nbsp;/, '');
            // ^ can't do this, as it causes the cursor to jump

            const title = titleParagraphNg.text()
                // sometimes mediumjs doesn't remove the placeholder nbsp properly.
                .replace(/^&nbsp;/, '')
                .trim();
            this.draft.title = title;
            console.log("draft-title: ",this.draft.title);

            //ADD TAGS
            const titleTags = title? title.match(/#(\S+)/gi) : [];
            const descriptionTags = description? description.match(/#(\S+)/gi) : [];

            tags = angular.element.unique(
                angular.element.merge(
                    titleTags ? titleTags : [],
                    descriptionTags ? descriptionTags : []
                )
            );

            for(var i=0; i<tags.length; i++){
                tags[i] = tags[i].substr(1);
            }

            this.draft.tags = tags && tags.length > 0? tags : undefined;
            console.log("draft-tags: ",this.draft.tags);
        }

        value() {
            return this.medium
                .value()
                /*
                 * the replace fixes odd behaviour of FF. it inserts
                 * a `<br>` at the end after the first space is
                 * typed -- unless the space is the first character
                 * in the field.
                 */
                .replace(/<br>$/, '');
        }

        initMedium() {
            // initialising editor. see http://jakiestfu.github.io/Medium.js/docs/
            this.medium = new Medium({
                element: this.mediumMount(),

                modifier: 'auto',
                placeholder: 'What',
                autoHR: false, //if true, inserts <hr> after two empty lines
                mode: Medium.partialMode, // allows newlines, no styling
                attributes: {
                    //remove: ['style', 'class'] //TODO does this remove the ng-class?
                    remove: ['style'] //TODO does this remove the ng-class?
                },
                beforeInsertHtml: function () {
                    //this = Medium.Html (!)

                    // Replace `<br>`s with the `<p>`s we use for line-breaking, to allow
                    // multi-line pasting. This assumes that pasting happens inside
                    // a `<p>` element and will horribly fail otherwise.
                    // The trimming happens, as leading whitespaces are removed in other
                    // lines as well during pasting.
                    const originalHtml = this.html;
                    const sanitizedHtml = originalHtml.trim().replace(/<br>/g, '</p><p>');
                    this.html = sanitizedHtml;

                    console.log('medium.js - beforeInsertHtml: ', this, originalHtml, sanitizedHtml);
                },
            });

            //remove the inline-styles placed by medium.js
            this.medium.placeholder.style = "";
            this.mediumMount().addEventListener('blur', e =>
                delay(0) //push to end end of task-queue (and thus all other `blur`-listeners
                    .then(() => {
                        const style = this.medium.placeholder.style;
                        style.minHeight = 0;
                        style.minWidth = 0;
                    })
            );
        }

        mediumMountNg() {
            if(!this._mediumMount) {
                this._mediumMount = this.textFieldNg().find('.medium-mount')
            }
            return this._mediumMount;
        }

        mediumMount() {
            return this.mediumMountNg()[0];
        }

        textFieldNg() {
            if(!this._textField) {
                this._textField = this.$element.find('.wdt__text');
            }
            return this._textField;
        }
        textField() {
            return this.textFieldNg()[0];
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { draft: "="},
        template: template
    }
}


export default angular.module('won.owner.components.needTextfield', [ ])
    .directive('needTextfield', genComponentConf)
    .name;