import { constantRoutes } from "@/router";

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes;
      state.routes = constantRoutes.concat(routes);
    },
    SET_SIDEBAR_ROUTERS: (state, routers) => {
      state.sidebarRouters = constantRoutes.concat(routers);
    }
  },
  actions: {
    // 生成路由
    GenerateRoutes({ commit }) {
      return new Promise(resolve => {
        // v2: fixed routes only. Disable legacy template dynamic routes.
        const rewriteRoutes = [{ path: "*", redirect: "/404", hidden: true }];
        commit("SET_ROUTES", rewriteRoutes);
        commit("SET_SIDEBAR_ROUTERS", []);
        resolve(rewriteRoutes);
      });
    }
  }
};

export default permission;
