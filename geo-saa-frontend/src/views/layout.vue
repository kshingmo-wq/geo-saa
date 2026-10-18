<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="layout-aside" :class="{ collapsed: sidebarCollapsed }">
      <div class="logo-container" @click="router.push('/dashboard')">
        <img src="@/assets/logo.svg" alt="logo" class="logo-img" />
        <span v-show="!sidebarCollapsed" class="logo-text">GEO-SaaS</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        :router="true"
        :unique-opened="true"
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>Dashboard</template>
        </el-menu-item>
        <el-menu-item index="/diagnose">
          <el-icon><Search /></el-icon>
          <template #title>诊断中心</template>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Notebook /></el-icon>
          <template #title>知识库</template>
        </el-menu-item>
        <el-menu-item index="/content">
          <el-icon><Edit /></el-icon>
          <template #title>创作中心</template>
        </el-menu-item>
        <el-menu-item index="/asset">
          <el-icon><FolderChecked /></el-icon>
          <template #title>资产存证</template>
        </el-menu-item>
        <el-menu-item index="/distribute">
          <el-icon><Share /></el-icon>
          <template #title>分发中心</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-menu-item index="/geo">
          <el-icon><MagicStick /></el-icon>
          <template #title>GEO/AAO 体检</template>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/system/permission">
            <el-icon><Lock /></el-icon>
            <template #title>权限管理</template>
          </el-menu-item>
          <el-menu-item index="/system/model-config">
            <el-icon><Cpu /></el-icon>
            <template #title>模型配置</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar" :size="20">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="32"><UserFilled /></el-avatar>
              <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const currentTitle = computed(() => route.meta?.title)

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/system/')) return path
  return path
})

function toggleSidebar() {
  appStore.toggleSidebar()
}

async function handleCommand(command) {
  if (command === 'logout') {
    // 需等待登出接口返回后再跳转，否则 token 被提前清除会导致登出请求失败
    await userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.getInfo().catch(() => {})
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

/* 深蓝而非纯黑：纯黑 (#000) 配高饱和选中块对比过硬，长时间注视更累 */
.layout-aside {
  --geo-menu-active: #1677ff;
  --geo-menu-hover: rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, #0d2137 0%, #0a1a2c 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(15, 34, 58, 0.12);
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}
.logo-img {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}
.logo-text {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.4px;
  white-space: nowrap;
}

.side-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  /* Element Plus 默认深色菜单底色与自定义背景冲突，清掉由 CSS 接管 */
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.72);
  --el-menu-active-color: #fff;
  --el-menu-hover-bg-color: var(--geo-menu-hover);
  padding: 8px;
}

/* 滚动条：默认样式在深色底上几乎不可见 */
.side-menu::-webkit-scrollbar { width: 6px; }
.side-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.16);
  border-radius: 3px;
}
.side-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.28);
}
.side-menu::-webkit-scrollbar-track { background: transparent; }

/* 选中态：左侧高亮竖条 + 主色底，比纯白块更克制，也保留了位置感 */
.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, var(--geo-menu-active) 0%, rgba(22, 119, 255, 0.75) 100%);
  border-radius: 6px;
  font-weight: 500;
}
.side-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: #fff;
  border-radius: 0 2px 2px 0;
}
.side-menu :deep(.el-menu-item) {
  border-radius: 6px;
  margin-bottom: 2px;
  height: 44px;
  line-height: 44px;
}
.side-menu :deep(.el-sub-menu__title) {
  border-radius: 6px;
  height: 44px;
  line-height: 44px;
}
.side-menu:not(.el-menu--collapse) { width: 220px; }

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-lighter);
  height: 60px;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(15, 34, 58, 0.04);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.collapse-btn {
  cursor: pointer;
  color: var(--el-text-color-regular);
  padding: 6px;
  border-radius: 6px;
  transition: background 0.2s ease, color 0.2s ease;
}
.collapse-btn:hover {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.header-right {
  display: flex;
  align-items: center;
}
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  padding: 6px 10px;
  border-radius: 8px;
  transition: background 0.2s ease;
}
.user-dropdown:hover { background: var(--el-fill-color-light); }
.username {
  font-size: 14px;
  font-weight: 500;
}

.layout-main {
  /* 与侧栏渐变同一色系，避免大面积灰显得沉闷 */
  background-color: #f4f6fa;
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.18s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .fade-enter-active, .fade-leave-active,
  .stat-card, .layout-aside, .collapse-btn { transition: none; }
}
</style>