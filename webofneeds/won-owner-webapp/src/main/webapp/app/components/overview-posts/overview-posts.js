;
import Immutable from 'immutable';
import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';
import {
    selectAllOwnNeeds,
} from '../../selectors';
import won from '../../won-es6';

const ZERO_UNSEEN = Object.freeze({
    matches: 0,
    incomingRequests: 0,
    conversations: 0,
});

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class OverviewPostsController {

    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 1;
        window.ovp4dbg = this;
        this.activePostsOpen = true;
        this.closedPostsOpen = false;

        const selectFromState = (state) => {
            const ownNeeds = selectAllOwnNeeds(state);

            let activePosts = ownNeeds.filter(post =>
                post.get("state") === won.WON.ActiveCompacted
            );
            activePosts = activePosts? activePosts.toArray() : [];

            let inactivePosts = ownNeeds.filter(post =>
                post.get("state") === won.WON.InactiveCompacted
            );
            inactivePosts = inactivePosts? inactivePosts.toArray() : [];

            return {
                activePostsUris: activePosts.map(p => p.get('uri')),
                activePostsCount: activePosts.length,
                inactivePostsUris: inactivePosts.map(p => p.get('uri')),
                inactivePostsCount: inactivePosts.length,
            }
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }


}

OverviewPostsController.$inject = [];

export default angular.module('won.owner.components.overviewPosts', [
        overviewTitleBarModule,
        postItemLineModule
    ])
    .controller('OverviewPostsController',[...serviceDependencies,OverviewPostsController] )

    .name;


