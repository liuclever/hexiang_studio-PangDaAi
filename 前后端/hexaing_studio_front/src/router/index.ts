import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { isLoggedIn } from '../utils/auth';
import { ElMessage } from 'element-plus';

// 布局和页面
const AppLayout = () => import('../views/AppLayout.vue');
const HomeView = () => import('../views/HomeView.vue');
const LoginView = () => import('../views/LoginView.vue');
const CoursesView = () => import('../views/CoursesView.vue');
const UserManagementView = () => import('../views/UserManagementView.vue');
const AnnouncementsView = () => import('../views/AnnouncementsView.vue');
const InfoPortalView = () => import('../views/InfoPortalView.vue');
const MaterialsView = () => import('../views/MaterialsView.vue');
const ProfileView = () => import('../views/ProfileView.vue');
const AccountSettingsView = () => import('../views/AccountSettingsView.vue');

// 路由配置
const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: HomeView,
        meta: { requiresAuth: true }
      },
      {
        path: 'courses',
        name: 'Courses',
        component: CoursesView,
        meta: { requiresAuth: true }
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('../views/AttendanceView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: UserManagementView,
        meta: { requiresAuth: true }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: AnnouncementsView,
        meta: { requiresAuth: true }
      },
      {
        path: 'info-portal',
        name: 'InfoPortal',
        component: InfoPortalView,
        meta: { requiresAuth: true }
      },
      {
        path: 'materials',
        name: 'Materials',
        component: MaterialsView,
        meta: { requiresAuth: true }
      },

      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView,
        meta: { requiresAuth: true }
      },
      {
        path: 'account-settings',
        name: 'AccountSettings',
        component: AccountSettingsView,
        meta: { requiresAuth: true }
      },
      {
        path: '/notifications',
        name: 'Notifications',
        component: () => import('../views/NotificationsView.vue'),
        meta: { requiresAuth: true, title: '通知中心' }
      },
      {
        path: '/system-settings',
        name: 'SystemSettings',
        component: () => import('../views/SystemSettingsView.vue'),
        meta: { requiresAuth: true, title: '系统设置' }
      }
    ]
  },
  {
    path: '/task',
    name: 'task',
    component: () => import('../views/AppLayout.vue'),
    meta: {
      requiresAuth: true,
      title: '任务管理'
    },
    redirect: '/task/management',
    children: [
      {
        path: 'management',
        name: 'TaskManagementPage',
        component: () => import('../views/TaskManagementView.vue'),
        meta: {
          requiresAuth: true,
          title: '任务管理'
        }
      },
      {
        path: 'approval',
        name: 'TaskApproval',
        component: () => import('../views/TaskApprovalView.vue'),
        meta: {
          requiresAuth: true,
          title: '任务审批'
        }
      }
    ]
  },
  {
    path: '/approval',
    name: 'approval',
    component: () => import('../views/AppLayout.vue'),
    meta: {
      requiresAuth: true,
      title: '审批中心'
    },
    redirect: '/approval/leave',
    children: [
      {
        path: 'leave',
        name: 'LeaveApproval',
        component: () => import('../views/attendance/LeaveApprovalView.vue'),
        meta: {
          requiresAuth: true,
          title: '请假审批'
        }
      }
    ]
  },
  // 404路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const loggedIn = isLoggedIn();
  
  // 增强的路由跳转日志
  console.log('路由导航:', {
    from: from.path,
    to: to.path,
    requiresAuth,
    loggedIn,
    matchedRoutes: to.matched.map(r => ({ 
      path: r.path, 
      name: r.name,
      component: r.components?.default?.name || 'Anonymous'
    }))
  });
  
  // 如果需要认证且未登录
  if (requiresAuth && !loggedIn) {
    ElMessage.warning('请先登录');
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } 
  // 如果已登录且访问登录页
  else if (loggedIn && to.name === 'Login') {
    next({ name: 'Home' });
  } 
  else {
    next();
  }
});

export default router; 