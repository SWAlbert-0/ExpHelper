import { getInfo, login, logout } from "@/api/vadmin/login";
import { getToken, removeToken, setToken } from "@/utils/auth";
import { userInfo } from "@/mock/index";

const user = {
  state: {
    token: getToken(),
    name: "",
    avatar: "",
    roles: [],
    permissions: [],
    unread_msg_count: 0
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token;
    },
    SET_NAME: (state, name) => {
      state.name = name;
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar;
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles;
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions;
    },
    SET_UNREAD_MSG_COUNT: (state, unread_msg_count) => {
      state.unread_msg_count = unread_msg_count;
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const code = userInfo.code;
      const uuid = userInfo.uuid;
      return new Promise((resolve, reject) => {
        let token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic2Vzc2lvbl9pZCI6IjUxYzc1NzMwLWY3NzktNDNiZC04OGViLWI0ODE5ZjYyOGQxZSIsImV4cCI6MTYyODE0NTMzOSwib3JpZ19pYXQiOjE2MjgwNTg5Mzl9.t5u2y5XyjBQwz-TnlDFKdCaEabE9SFV8suY4ssyVt8g";
        setToken(token);
        commit("SET_TOKEN", token);
        resolve();
        // login(username, password, code, uuid).then(res => {
        //   setToken(res.data.token);
        //   commit("SET_TOKEN", res.data.token);
        //   resolve();
        // }).catch(error => {
        //   reject(error);
        // });
      });
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        const res = userInfo;
        const user = res.data.user;
        const avatar = user.avatar ? process.env.VUE_APP_BASE_API + user.avatar : require("@/assets/images/profile.jpg");
        if (res.data.roles && res.data.roles.length > 0) { // 验证返回的roles是否是一个非空数组
          commit("SET_ROLES", res.data.roles);
          commit("SET_PERMISSIONS", res.data.permissions);
        } else {
          commit("SET_ROLES", ["ROLE_DEFAULT"]);
        }
        commit("SET_NAME", user.name);
        commit("SET_UNREAD_MSG_COUNT", user.unread_msg_count);
        commit("SET_AVATAR", avatar);
        resolve(res.data);

        // getInfo(state.token).then(res => {
        //   const user = res.data.user;
        //   const avatar = user.avatar ? process.env.VUE_APP_BASE_API + user.avatar : require("@/assets/images/profile.jpg");
        //   if (res.data.roles && res.data.roles.length > 0) { // 验证返回的roles是否是一个非空数组
        //     commit("SET_ROLES", res.data.roles);
        //     commit("SET_PERMISSIONS", res.data.permissions);
        //   } else {
        //     commit("SET_ROLES", ["ROLE_DEFAULT"]);
        //   }
        //   commit("SET_NAME", user.name);
        //   commit("SET_UNREAD_MSG_COUNT", user.unread_msg_count);
        //   commit("SET_AVATAR", avatar);
        //   resolve(res.data);
        // }).catch(error => {
        //   reject(error);
        // });
      });
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit("SET_TOKEN", "");
          commit("SET_ROLES", []);
          commit("SET_PERMISSIONS", []);
          removeToken();
          resolve();
        }).catch(error => {
          reject(error);
        });
      });
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit("SET_TOKEN", "");
        removeToken();
        resolve();
      });
    }
  }
};

export default user;
