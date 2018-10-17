import angular from "angular";
import { Elm } from "../../elm/PublishButton.elm";
import "./svg-icon.js";
import { currentSkin, getPersonas } from "../selectors";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      isValid: "=",
      onPublish: "&",
    },
    link: (scope, element) => {
      const elmApp = Elm.PublishButton.init({
        node: element[0],
        flags: {
          width: window.innerWidth,
          height: window.innerHeight,
          skin: currentSkin(),
        },
      });

      scope.$watch("isValid", newValue => {
        elmApp.ports.publishIn.send({
          draftValid: newValue ? true : false,
          loggedIn: $ngRedux.getState().getIn(["user", "loggedIn"]),
        });
      });

      elmApp.ports.publishIn.send({
        draftValid: scope.isValid ? true : false,
        loggedIn: $ngRedux.getState().getIn(["user", "loggedIn"]),
      });

      const personas = getPersonas($ngRedux.getState().get("needs"));
      if (personas) {
        elmApp.ports.personaIn.send(personas.toJS());
      }

      const disconnectOptions = $ngRedux.connect(state => {
        return {
          personas: getPersonas(state.get("needs")),
          loggedIn: state.getIn(["user", "loggedIn"]),
        };
      })(state => {
        elmApp.ports.publishIn.send({
          draftValid: scope.isValid ? true : false,
          loggedIn: state.loggedIn,
        });
        if (!state.personas) {
          return;
        }
        elmApp.ports.personaIn.send(state.personas.toJS());
      });

      const disconnectSkin = $ngRedux.connect(state => {
        return {
          skin: state.getIn(["config", "theme"]),
        };
      })(() => {
        elmApp.ports.skin.send(currentSkin());
      });

      elmApp.ports.publishOut.subscribe(url => {
        scope.onPublish({ persona: url });
      });

      scope.$on("$destroy", () => {
        disconnectOptions();
        disconnectSkin();
        elmApp.ports.publishOut.unsubscribe();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.publishButton", [])
  .directive("wonPublishButton", genComponentConf).name;
