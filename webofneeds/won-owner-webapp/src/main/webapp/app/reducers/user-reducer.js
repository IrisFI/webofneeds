/**
 * Created by ksinger on 10.05.2016.
 */
import { actionTypes } from '../actions/actions.js';
import { repeatVar } from '../utils.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils.js';
import { buildCreateMessage } from '../won-message-utils.js';
import won from '../won-es6.js';

const initialState = Immutable.fromJS({loggedIn: false});

export default function(userData = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.initialPageLoad:
        case actionTypes.login:
            //because we get payload as immutablejs-map sometimes but not always
            var immutablePayload = Immutable.fromJS(action.payload);

            var loggedIn = immutablePayload.get('loggedIn');
            var email = immutablePayload.get('email');

            if(loggedIn){
                return Immutable.fromJS({loggedIn: true, email: email});
            } else {
                return userData;
            }

        case actionTypes.logout:
            return Immutable.fromJS({loggedIn: false});

        case actionTypes.loginFailed:
            return Immutable.fromJS({loginError: action.payload.loginError, loggedIn: false});

        case actionTypes.typedAtLoginCredentials:
            if(!userData.get("loggedIn")){
                return userData.set('loginError', undefined);
            } else {
                return userData
            }

        case actionTypes.registerReset:
            return Immutable.fromJS({registerError: undefined});

        case actionTypes.registerFailed:
            return Immutable.fromJS({registerError: action.payload.registerError});

        default:
            return userData;
    }
}
