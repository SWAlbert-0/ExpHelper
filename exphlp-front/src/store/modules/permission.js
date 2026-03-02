import { constantRoutes } from "@/router";

function hasRolePermission(route, roles) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role));
  }
  return true;
}

function filterRoutesByRoles(routes, roles) {
  const res = [];
  routes.forEach(route => {
    const tmp = { ...route };
    if (hasRolePermission(tmp, roles)) {
      if (tmp.children) {
        tmp.children = filterRoutesByRoles(tmp.children, roles);
      }
      res.push(tmp);
    }
  });
  return res;
}

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
      state.sidebarRouters = routers;
    }
  },
  actions: {
    // 生成路由
    GenerateRoutes({ commit }, data = {}) {
      return new Promise(resolve => {
        // v2: fixed routes only. Disable legacy template dynamic routes.
        const roles = Array.isArray(data.roles) ? data.roles : [];
        const rewriteRoutes = [{ path: "*", redirect: "/404", hidden: true }];
        const filteredConstantRoutes = filterRoutesByRoles(constantRoutes, roles);
        commit("SET_ROUTES", rewriteRoutes);
        commit("SET_SIDEBAR_ROUTERS", filteredConstantRoutes);
        resolve(rewriteRoutes);
      });
    }
  }
};

export default permission;
