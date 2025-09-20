Component({
  data: {
    selected: 1, // 默认选中工作台
    color: "#909399",
    selectedColor: "#1890FF",
    list: []
  },

  attached() {
    // 初始化TabBar列表
    this.initTabBarList();
    // 获取当前页面路径，设置选中状态
    this.setSelected();
  },

  methods: {
    initTabBarList() {
      // 获取用户角色
      const role = wx.getStorageSync('role') || 'student';
      
      let tabBarList = [
      {
        pagePath: "pages/message/index",
        text: "公告",
        iconName: "notification",
        selectedIconName: "notification"
      },
      {
        pagePath: "pages/index/index", 
        text: "工作台",
        iconName: "desktop",
        selectedIconName: "desktop"
        }
      ];

      // 根据角色添加签到页面
      if (role !== 'admin') {
        tabBarList.push({
        pagePath: "pages/attendance/check-in",
        text: "签到", 
        iconName: "location",
        selectedIconName: "location"
        });
      }

      // 添加通用页面
      tabBarList.push(
      {
        pagePath: "pages/user/index",
        text: "成员",
        iconName: "usergroup",
        selectedIconName: "usergroup"
      },
      {
        pagePath: "pages/profile/index",
        text: "我的",
        iconName: "user",
        selectedIconName: "user"
      }
      );

      this.setData({
        list: tabBarList
      });
  },

    switchTab(e) {
      const data = e.currentTarget.dataset;
      const url = `/${data.path}`;
      
      // 添加点击动画效果
      this.triggerAnimation(data.index);
      
      // 切换页面
      wx.switchTab({
        url,
        success: () => {
          this.setData({
            selected: data.index
          });
        }
      });
    },

    setSelected() {
      // 根据当前页面设置选中项
      const pages = getCurrentPages();
      if (pages.length === 0) return;
      
      const currentPage = pages[pages.length - 1];
      const route = currentPage.route;
      
      const selectedIndex = this.data.list.findIndex(item => {
        return item.pagePath === route;
      });
      
      if (selectedIndex !== -1) {
        this.setData({
          selected: selectedIndex
        });
      }
    },

    // 提供外部调用方法
    updateSelected(index) {
      this.setData({
        selected: index
      });
    },

    triggerAnimation(index) {
      // 触发点击动画
      const animation = wx.createAnimation({
        duration: 200,
        timingFunction: 'ease-out'
      });
      
      animation.scale(0.9).step();
      animation.scale(1.1).step();
      animation.scale(1).step();
      
      this.setData({
        [`animationData${index}`]: animation.export()
      });
      
      // 清除动画
      setTimeout(() => {
        this.setData({
          [`animationData${index}`]: {}
        });
      }, 600);
    }
  }
}); 