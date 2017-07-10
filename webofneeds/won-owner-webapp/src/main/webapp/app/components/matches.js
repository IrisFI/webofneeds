;

import Immutable from 'immutable';
import won from '../won-es6';
import angular from 'angular';
import overviewTitleBarModule from './overview-title-bar';
import matchesFlowItemModule from './matches-flow-item';
import matchesGridItemModule from './matches-grid-item';
import sendRequestModule from './send-request';
import connectionsOverviewModule from './connections-overview';
import connectionSelectionModule from './connection-selection';

import { attach, decodeUriComponentProperly} from '../utils';
import { labels } from '../won-label-utils';
import { actionCreators }  from '../actions/actions';
import {
    selectOpenPostUri,
    displayingOverview,
} from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
let template = `
    <a class="curtain" ng-if="self.connection"></a>
    <div class="omc__inner" ng-class="{'empty' : !self.hasMatches}">
        <div class="omc__empty" ng-if="!self.hasMatches">
            <div class="omc__empty__description">
                <img src="generated/icon-sprite.svg#ico36_match_grey" class="omc__empty__description__icon">
                <span class="omc__empty__description__text">The matches to all your needs will be listed here. 
                 You cannot influence the matching process. It might take some time, or maybe there is nothing to
                    be found for you, yet. Check back later or post more needs!</span>
            </div>
            <a ui-sref="createNeed" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_plus" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Create a Need</span>
            </a>
        </div>
        <div class="omc__header" ng-if="self.hasMatches">
            <div class="title">Matches to your post{{ self.isOverview? 's' : '' }}</div>
            <div class="omc__header__viewtype">
                <a ui-sref="{layout: self.LAYOUT.TILES}">
                    <img ng-src="{{self.layout === 'tiles' ? 'generated/icon-sprite.svg#ico-filter_tile_selected' : 'generated/icon-sprite.svg#ico-filter_tile'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{layout: self.LAYOUT.GRID}">
                    <img ng-src="{{self.layout === 'grid' ? 'generated/icon-sprite.svg#ico-filter_compact_selected' : 'generated/icon-sprite.svg#ico-filter_compact'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{layout: self.LAYOUT.LIST}">
                    <img ng-src="{{self.layout === 'list' ? 'generated/icon-sprite.svg#ico-filter_list_selected' : 'generated/icon-sprite.svg#ico-filter_list'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
            </div>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'tiles'" class="omc__content__flow">
            <won-matches-flow-item
                    connection-uri="m.get('uri')"
                    ng-repeat="m in self.matches">
            </won-matches-flow-item>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'grid'" class="omc__content__grid">
            <won-matches-grid-item
                    connection-uri="m.get('uri')"
                    ng-repeat="m in self.matches">
            </won-matches-grid-item>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'list'" class="omc__content__list">
            <won-connections-overview
                ng-show="self.isOverview"
                connection-type="::self.WON.Suggested"
                on-selected-connection="self.selectedConnection(connectionUri)">
            </won-connections-overview>

            <won-connection-selection
                ng-show="!self.isOverview"
                connection-type="::self.WON.Suggested"
                on-selected-connection="self.selectedConnection(connectionUri)">
            </won-connection-selection>
        </div>
    </div>
    <div class="omc__sendrequest" ng-if="self.hasMatches && self.connection">
        <won-send-request></won-send-request>
    </div>
`

const LAYOUT = Object.freeze({ TILES: 'tiles', GRID: 'grid', LIST: 'list'});

class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc4dbg = this;

        this.WON = won.WON;
        this.LAYOUT = LAYOUT;
        this.labels = labels;

        const selectFromState = (state) => {
            const postUri = selectOpenPostUri(state);
            const connectionUri = decodeUriComponentProperly(state.getIn(['router', 'currentParams', 'connectionUri']));


            // either of 'tiles', 'grid', 'list'
            let layout = state.getIn(['router','currentParams','layout']);
            if(!layout) {
                layout = 'tiles';
            }

            const isOverview = displayingOverview(state);
            let matchesByConnectionUri;
            if(isOverview) { //overview
                let allMatchesByConnections = Immutable.Map();

                state.getIn(["needs", "allNeeds"]).filter(need => need.get("ownNeed")).map(function(need, key){
                    allMatchesByConnections.merge(need.get("connections").filter(conn => conn.get("state") === won.WON.Suggested));
                });

                matchesByConnectionUri = allMatchesByConnections.toList();
            } else { // post-owner view
                matchesByConnectionUri = state.getIn(["needs", "allNeeds", postUri, "connections"])
                    .filter(conn => conn.get("state") === won.WON.Suggested)
                    .toList();
            }

            return {
                isOverview,
                layout,
                //LAYOUT,
                connection: state.getIn(["needs", "allNeeds", postUri, 'connections', connectionUri]),
                matches: matchesByConnectionUri.toArray(),
                hasMatches: matchesByConnectionUri.size > 0,
                //post: state.getIn(['needs','allNeeds', postUri]),
            };
        };
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
      //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }

    loadMatches(){
        this.matches__load(
            this.$ngRedux.getState().getIn(['needs','ownNeeds']).toJS()
        )
    }

    selectedConnection(connectionUri) {
        if(this.isOverview) {
            this.router__stateGo('overviewMatches', {connectionUri});
        } else {
            this.router__stateGo('post', {connectionUri});
        }
    }
}
Controller.$inject = serviceDependencies;

function genComponentConf() {
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.matches', [
        overviewTitleBarModule,
        matchesFlowItemModule,
        matchesGridItemModule,
        sendRequestModule,
        connectionsOverviewModule,
        connectionSelectionModule,
    ])
    .directive('wonMatches', genComponentConf)
    //.controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

