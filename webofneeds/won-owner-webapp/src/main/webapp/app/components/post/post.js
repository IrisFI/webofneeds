/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ownerTitleBarModule from '../owner-title-bar';
//import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { attach, mapToMatches, decodeUriComponentProperly } from '../../utils';
import won from '../../won-es6';
import { actionCreators }  from '../../actions/actions';
import openRequestModule from '../open-request';
import connectionSelectionModule from '../connection-selection';
import postInfoModule from '../post-info';
import matchesModule from '../matches';
import {
    selectAllByConnections,
    selectOpenPostUri,
    selectOpenConnectionUri,
    selectOpenPost,
    selectOwningOpenPost
} from '../../selectors';
import { relativeTime } from '../../won-label-utils';

const serviceDependencies = ['$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        window.p4dbg = this;
        this.wonConnected = won.WON.Connected;

        const selectFromState = (state)=>{
            const postUri = selectOpenPostUri(state);

            const connectionUri = selectOpenConnectionUri(state);
            const actualConnectionType = state.getIn([
                'connections', connectionUri, 'hasConnectionState'
            ]);

            const connectionTypeInParams = decodeUriComponentProperly(state.getIn(['router', 'currentParams', 'connectionType']));

            const connectionIsOpen = !!connectionUri &&
                //make sure we don't get a mismatch between supposed type and actual type:
                actualConnectionType == connectionTypeInParams;

            return {
                postUri: selectOpenPostUri(state),
                post: selectOpenPost(state),
                owningPost: selectOwningOpenPost(state),
                connectionUri,
                connectionType: connectionTypeInParams,
                showConnectionSelection: !!connectionTypeInParams && connectionTypeInParams !== won.WON.Suggested,
                showMatches: connectionTypeInParams === won.WON.Suggested,
                showConversationDetails: connectionIsOpen && connectionTypeInParams === won.WON.Connected,
                showIncomingRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestReceived,
                showSentRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestSent,
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
    openConnection(connectionUri) {
        this.router__stateGo('post', {
            postUri: decodeURIComponent(this.postUri),
            connectionUri: connectionUri,
            connectionType: this.connectionType,
        })
    }
}

Controller.$inject = serviceDependencies;



export default angular.module('won.owner.components.post', [
    ownerTitleBarModule,
    //galleryModule,
    postMessagesModule,
    connectionSelectionModule,
    openRequestModule,
    matchesModule,
    postInfoModule,
])
    .controller('PostController', Controller)
    .name;
