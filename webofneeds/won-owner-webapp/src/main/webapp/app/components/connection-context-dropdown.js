/**
 * Created by ksinger on 30.03.2017.
 */

import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import won from "../won-es6.js";
import { attach } from "../utils.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
} from "../selectors.js";
import { connect2Redux } from "../won-utils.js";

import "style/_context-dropdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
            <svg class="cdd__icon__small"
                ng-if="self.isLoading()"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <svg class="cdd__icon__small clickable"
                ng-if="!self.isLoading()"
                style="--local-primary:var(--won-secondary-color);"
                ng-click="self.contextMenuOpen = true">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <div class="cdd__contextmenu" ng-show="self.contextMenuOpen">
                <div class="cdd__contextmenu__content" ng-click="self.contextMenuOpen = false">
                    <div class="topline">
                        <svg class="cdd__icon__small__contextmenu clickable"
                            style="--local-primary:black;">
                            <use  xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
                        </svg>
                    </div>
                    <!-- Buttons when connection is available -->
                    <button
                        class="won-button--outlined thin red"
                        ng-if="!self.isSuggested"
                        ng-click="self.goToPost(self.connection.get('remoteNeedUri'))">
                        Show Details
                    </button>
                    <button class="won-button--outlined thin red show-in-responsive"
                        ng-click="self.toggleRdfDisplay()">
                        <svg class="won-button-icon" style="--local-primary:var(--won-primary-color);">
                            <use xlink:href="#ico36_rdf_logo" href="#ico36_rdf_logo"></use>
                        </svg>
                        <span>{{self.shouldShowRdf? "Hide raw RDF data" : "Show raw RDF data"}}</span>
                    </button>
                    <button
                        ng-if="self.isConnected && !self.showAgreementData"
                        class="won-button--outlined thin red"
                        ng-click="self.showAgreementDataField()">
                        Show Agreement Data
                    </button>
                    <button
                        ng-if="self.isConnected"
                        class="won-button--outlined thin red"
                        ng-click="self.toggleMultiSelect()">
                        {{ self.getMultiSelectText() }}
                    </button>
                    <button
                        ng-if="self.isConnected || self.isSuggested"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Remove Connection
                    </button>
                    <button
                        ng-if="self.isSentRequest"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Cancel Request
                    </button>
                </div>
            </div>
        `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const connectionUri = selectOpenConnectionUri(state);

        const post =
          connectionUri && selectNeedByConnectionUri(state, connectionUri);
        const connection = post && post.getIn(["connections", connectionUri]);
        const connectionState = connection && connection.get("state");

        return {
          connection,
          connectionUri,
          showAgreementData: connection && connection.get("showAgreementData"),
          showMultiSelect: connection && connection.get("showMultiSelect"),
          shouldShowRdf: state.get("showRdf"),
          isConnected: connectionState === won.WON.Connected,
          isSentRequest: connectionState === won.WON.RequestSent,
          isReceivedRequest: connectionState === won.WON.RequestReceived,
          isSuggested: connectionState === won.WON.Suggested,
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      const callback = event => {
        const clickedElement = event.target;
        //hide MainMenu if click was outside of the component and menu was open
        if (
          this.contextMenuOpen &&
          !this.$element[0].contains(clickedElement)
        ) {
          this.contextMenuOpen = false;
          this.$scope.$apply();
        }
      };

      this.$scope.$on("$destroy", () => {
        window.document.removeEventListener("click", callback);
      });

      window.document.addEventListener("click", callback);
    }

    isLoading() {
      return !this.connection || this.connection.get("isLoading");
    }

    closeConnection() {
      this.connections__close(this.connectionUri);
      this.router__stateGoCurrent({
        useCase: undefined,
        connectionUri: undefined,
      });
    }

    toggleMultiSelect() {
      this.connections__showMultiSelect({
        connectionUri: this.connectionUri,
        showMultiSelect: !this.showMultiSelect,
      });
    }

    getMultiSelectText() {
      return this.showMultiSelect ? "Close MultiSelect" : "Select Messages";
    }

    goToPost(postUri) {
      this.router__stateGoCurrent({ useCase: undefined, postUri: postUri });
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      showAgreementDataField: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionContextDropdown", [ngAnimate])
  .directive("wonConnectionContextDropdown", genComponentConf).name;
