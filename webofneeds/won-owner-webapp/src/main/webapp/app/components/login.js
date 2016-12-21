/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import {
    attach,
    delay,
} from '../utils';
import { actionCreators }  from '../actions/actions';

function genLoginConf() {
    let template = `<a class="wl__button" ng-click="self.hideLogin()">
                        <span class="wl__button__caption">Sign in</span>
                        <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" class="wl__button__carret">
                    </a>
                    <!-- <div ng-form="loginForm">-->
                    <form ng-submit="::self.login(self.email, self.password)" id="loginForm" class="loginForm">
                        <input
                            id="loginEmail"
                            placeholder="Email address"
                            ng-model="self.email"
                            type="email"
                            required
                            autofocus
                            ng-keyup="self.loginReset() || ($event.keyCode == 13 && self.login(self.email, self.password))"/>
                        <span class="wl__errormsg">
                            {{self.loginError}}
                        </span>
                        <input
                            id="loginPassword"
                            placeholder="Password"
                            ng-model="self.password"
                            type="password"
                            required
                            ng-keyup="self.loginReset() || ($event.keyCode == 13 && self.login(self.email, self.password))"/>

                        <!-- <input type="submit" value="LOGIN"/>-->
                        <button
                            class="won-button--filled lighterblue"
                            ng-disabled="loginForm.$invalid">
                            <!--ng-click="::self.login(self.email, self.password)">-->
                                Sign in
                        </button>
                    </form>
                    <!-- TODO: Implement remember me and forgot password --><!--div class="wl__table">
                        <div class="wlt__left">
                            <input type="checkbox" ng-model="self.rememberMe" id="rememberMe"/><label for="rememberMe">Remember me</label>
                        </div>
                        <div class="wlt__right">
                            <a href="#">Forgot Password?</a>
                        </div>
                    </div>-->
                    <div class="wl__register">
                        No Account yet?
                        <a ui-sref="landingpage({focusSignup: true})">
                            Sign up
                        </a>
                    </div>`;

    const serviceDependencies = ['$q', '$ngRedux', '$scope', '$element' /*'$routeParams' /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);

            window.lic4dbg = this;

            this.email = "";
            this.password = "";

            const login = (state) => ({
                loginVisible: state.get('loginVisible'),
                loggedIn: state.getIn(['user','loggedIn']),
                loginError: state.getIn(['user','loginError'])
            });

            const disconnect = this.$ngRedux.connect(login, actionCreators)(this);
            this.$scope.$on('$destroy',disconnect);


            this.autofillHack();

            this.$scope.$watch(() => this.loginVisible, (newVis, oldVis) => {
                console.log('lic4dbg loginVisible: ', oldVis, newVis);
                //if(newVis && !oldVis) {
                this.autofillHack();
                //}
            });
        }

        /**
         * auto-fill hack. firefox doesn't fire an input event when auto-filling,
         * so we do this here manually to make sure the ng-model updates itself.
         */
        autofillHack() {
            return delay(0).then(() => {
                this.$element.find('#loginEmail').trigger('input');
                this.$element.find('#loginPassword').trigger('input');
                delay(100).then(() => console.log("lic4dbg email: ", this.email));
            });

        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {open: '='},
        template: template
    }
}

export default angular.module('won.owner.components.login', [])
    .directive('wonLogin', genLoginConf)
    .name;

