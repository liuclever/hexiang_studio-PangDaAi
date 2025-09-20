// 模拟状态枚举
export const TaskStatus = {
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  OVERDUE: 'OVERDUE',
  URGENT: 'URGENT',
  PENDING_REVIEW: 'PENDING_REVIEW',
  REJECTED: 'REJECTED'
};

export const SubmissionStatus = {
  SUBMITTED: 'SUBMITTED',
  REVIEWED: 'REVIEWED',
  RETURNED: 'RETURNED',
  OVERDUE: 'OVERDUE'
};

// 模拟用户数据
export const mockUsers = [
  { userId: 1, name: '张三', role: '学生', department: '信息工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 2, name: '李四', role: '学生', department: '信息工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 3, name: '王五', role: '学生', department: '信息工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 4, name: '赵六', role: '学生', department: '信息工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 5, name: '钱七', role: '学生', department: '电子工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 6, name: '孙八', role: '学生', department: '电子工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 7, name: '周九', role: '学生', department: '计算机科学学院', avatar: '/images/default-avatar.svg' },
  { userId: 8, name: '吴十', role: '学生', department: '计算机科学学院', avatar: '/images/default-avatar.svg' },
  { userId: 9, name: '陈老师', role: '教师', department: '信息工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 10, name: '李老师', role: '教师', department: '计算机科学学院', avatar: '/images/default-avatar.svg' },
  { userId: 11, name: '刘一', role: '学生', department: '人工智能学院', avatar: '/images/default-avatar.svg' },
  { userId: 12, name: '陈二', role: '学生', department: '人工智能学院', avatar: '/images/default-avatar.svg' },
  { userId: 13, name: '张明', role: '学生', department: '软件工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 14, name: '王丽', role: '学生', department: '软件工程学院', avatar: '/images/default-avatar.svg' },
  { userId: 15, name: '黄教授', role: '教师', department: '人工智能学院', avatar: '/images/default-avatar.svg' },
  { userId: 16, name: '郑博士', role: '教师', department: '软件工程学院', avatar: '/images/default-avatar.svg' },
];

// 模拟任务数据
export const mockTasks = [
  {
    taskId: 1,
    title: '调研报告撰写',
    description: '完成人工智能在教育领域的应用调研报告，包括文献综述、案例分析和未来趋势预测',
    startTime: '2023-07-01T08:00:00',
    endTime: '2023-07-15T18:00:00',
    status: TaskStatus.COMPLETED,
    creator: { userId: 9, name: '陈老师', role: '教师' },
    createTime: '2023-06-28T10:30:00',
    completedSubTasks: 3,
    totalSubTasks: 3,
    subTasks: [
      {
        subTaskId: 101,
        title: '文献综述',
        description: '整理近五年关于人工智能在教育领域的主要研究文献',
        members: [
          {
            userId: 1,
            name: '张三',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 1001,
              submitTime: '2023-07-05T15:30:00',
              comment: '已完成文献综述，共收集整理了30篇相关文献',
              fileUrl: 'https://example.com/files/lit-review.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '整理得很全面，继续保持',
              reviewTime: '2023-07-06T09:15:00'
            }
          },
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 1002,
              submitTime: '2023-07-04T16:20:00',
              comment: '完成了国际期刊相关文献的整理',
              fileUrl: 'https://example.com/files/international-papers.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '很好，注意格式统一',
              reviewTime: '2023-07-06T09:20:00'
            }
          }
        ]
      },
      {
        subTaskId: 102,
        title: '案例分析',
        description: '选取3-5个典型案例，分析人工智能技术在教育中的具体应用',
        members: [
          {
            userId: 3,
            name: '王五',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 1003,
              submitTime: '2023-07-10T11:45:00',
              comment: '完成了5个案例的分析，包括智能辅导、自适应学习等领域',
              fileUrl: 'https://example.com/files/case-analysis.docx',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '案例选取很合适，分析到位',
              reviewTime: '2023-07-11T14:30:00'
            }
          }
        ]
      },
      {
        subTaskId: 103,
        title: '未来趋势预测',
        description: '基于当前研究和技术发展，预测未来3-5年内的发展趋势',
        members: [
          {
            userId: 4,
            name: '赵六',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 1004,
              submitTime: '2023-07-12T17:20:00',
              comment: '完成了趋势预测部分，重点关注了个性化学习和情感计算两个方向',
              fileUrl: 'https://example.com/files/future-trends.pptx',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '预测有一定的前瞻性，可以再补充一些数据支持',
              reviewTime: '2023-07-13T10:15:00'
            }
          }
        ]
      }
    ]
  },
  {
    taskId: 2,
    title: '移动应用开发项目',
    description: '设计并开发一个校园服务类移动应用，包括需求分析、UI设计、前后端开发和测试',
    startTime: '2023-07-10T08:00:00',
    endTime: '2023-08-10T18:00:00',
    status: TaskStatus.IN_PROGRESS,
    creator: { userId: 10, name: '李老师', role: '教师' },
    createTime: '2023-07-05T14:20:00',
    completedSubTasks: 2,
    totalSubTasks: 4,
    subTasks: [
      {
        subTaskId: 201,
        title: '需求分析',
        description: '调研校园服务需求，完成需求分析报告',
        members: [
          {
            userId: 5,
            name: '钱七',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 2001,
              submitTime: '2023-07-15T14:30:00',
              comment: '完成了问卷调查和访谈，整理了主要需求',
              fileUrl: 'https://example.com/files/requirements.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '需求调研很充分，分析到位',
              reviewTime: '2023-07-16T10:20:00'
            }
          }
        ]
      },
      {
        subTaskId: 202,
        title: 'UI设计',
        description: '完成应用的UI设计，包括原型图和设计稿',
        members: [
          {
            userId: 6,
            name: '孙八',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 2002,
              submitTime: '2023-07-20T16:45:00',
              comment: '完成了全部界面的设计稿',
              fileUrl: 'https://example.com/files/ui-design.fig',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '设计美观实用，交互逻辑清晰',
              reviewTime: '2023-07-21T11:30:00'
            }
          }
        ]
      },
      {
        subTaskId: 203,
        title: '前后端开发',
        description: '根据需求和设计，完成应用的前后端开发',
        members: [
          {
            userId: 7,
            name: '周九',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 2003,
              submitTime: '2023-07-30T18:20:00',
              comment: '前端部分已完成80%，还有部分页面需要优化',
              fileUrl: 'https://example.com/files/frontend-code.zip',
              status: SubmissionStatus.SUBMITTED,
              reviewComment: '',
              reviewTime: ''
            }
          },
          {
            userId: 8,
            name: '吴十',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 204,
        title: '测试与部署',
        description: '进行功能测试、性能测试，并完成应用部署',
        members: [
          {
            userId: 1,
            name: '张三',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  },
  {
    taskId: 3,
    title: '数据分析报告',
    description: '对学校近三年的教学评估数据进行分析，形成分析报告',
    startTime: '2023-06-15T08:00:00',
    endTime: '2023-06-30T18:00:00',
    status: TaskStatus.OVERDUE,
    creator: { userId: 9, name: '陈老师', role: '教师' },
    createTime: '2023-06-10T09:15:00',
    completedSubTasks: 1,
    totalSubTasks: 3,
    subTasks: [
      {
        subTaskId: 301,
        title: '数据收集与预处理',
        description: '收集整理近三年的教学评估数据，进行清洗和预处理',
        members: [
          {
            userId: 3,
            name: '王五',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 3001,
              submitTime: '2023-06-20T17:30:00',
              comment: '完成了数据收集和初步清洗',
              fileUrl: 'https://example.com/files/data-preprocessing.xlsx',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '数据处理很规范',
              reviewTime: '2023-06-21T10:40:00'
            }
          }
        ]
      },
      {
        subTaskId: 302,
        title: '数据分析与可视化',
        description: '使用统计方法和可视化工具进行数据分析',
        members: [
          {
            userId: 4,
            name: '赵六',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 3002,
              submitTime: '2023-07-02T10:15:00',
              comment: '完成了基础分析和部分可视化',
              fileUrl: 'https://example.com/files/data-analysis.ipynb',
              status: SubmissionStatus.OVERDUE,
              reviewComment: '提交已超时，但分析工作有一定质量',
              reviewTime: '2023-07-03T14:20:00'
            }
          }
        ]
      },
      {
        subTaskId: 303,
        title: '报告撰写',
        description: '根据分析结果撰写完整的分析报告',
        members: [
          {
            userId: 5,
            name: '钱七',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  },
  {
    taskId: 4,
    title: '学术论文投稿',
    description: '完成一篇关于深度学习在图像识别中应用的学术论文，并提交至指定期刊',
    startTime: '2023-08-01T08:00:00',
    endTime: '2023-09-30T18:00:00',
    status: TaskStatus.PENDING_REVIEW,
    creator: { userId: 10, name: '李老师', role: '教师' },
    createTime: '2023-07-25T15:45:00',
    completedSubTasks: 0,
    totalSubTasks: 4,
    subTasks: [
      {
        subTaskId: 401,
        title: '文献调研',
        description: '调研近期相关领域的研究进展',
        members: [
          {
            userId: 6,
            name: '孙八',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 402,
        title: '算法设计与实现',
        description: '设计并实现改进的图像识别算法',
        members: [
          {
            userId: 7,
            name: '周九',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 8,
            name: '吴十',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 403,
        title: '实验与结果分析',
        description: '进行对比实验，分析算法性能',
        members: [
          {
            userId: 1,
            name: '张三',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 404,
        title: '论文撰写与投稿',
        description: '完成论文撰写，并按期刊要求格式化后提交',
        members: [
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  },
  {
    taskId: 5,
    title: '智能机器人设计大赛',
    description: '设计并制作一个能够完成特定任务的智能机器人，参加校级比赛',
    startTime: '2023-09-01T08:00:00',
    endTime: '2023-11-30T18:00:00',
    status: TaskStatus.IN_PROGRESS,
    creator: { userId: 15, name: '黄教授', role: '教师' },
    createTime: '2023-08-15T10:30:00',
    completedSubTasks: 1,
    totalSubTasks: 4,
    subTasks: [
      {
        subTaskId: 501,
        title: '方案设计',
        description: '设计机器人的整体方案，包括硬件选型和软件架构',
        members: [
          {
            userId: 11,
            name: '刘一',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 5001,
              submitTime: '2023-09-15T16:40:00',
              comment: '完成了整体方案设计，包括机器人结构和控制系统',
              fileUrl: 'https://example.com/files/robot-design.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '方案设计合理，考虑了各种因素',
              reviewTime: '2023-09-16T11:20:00'
            }
          },
          {
            userId: 12,
            name: '陈二',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 5002,
              submitTime: '2023-09-14T15:30:00',
              comment: '完成了软件架构设计，采用模块化结构',
              fileUrl: 'https://example.com/files/software-architecture.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '架构设计清晰，接口定义合理',
              reviewTime: '2023-09-16T11:30:00'
            }
          }
        ]
      },
      {
        subTaskId: 502,
        title: '硬件组装',
        description: '采购零部件，完成机器人的硬件组装',
        members: [
          {
            userId: 11,
            name: '刘一',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 5003,
              submitTime: '2023-10-10T14:20:00',
              comment: '完成了基础硬件组装，还需要调整部分传感器位置',
              fileUrl: 'https://example.com/files/hardware-assembly.jpg',
              status: SubmissionStatus.SUBMITTED,
              reviewComment: '',
              reviewTime: ''
            }
          }
        ]
      },
      {
        subTaskId: 503,
        title: '软件开发',
        description: '开发机器人的控制软件和算法',
        members: [
          {
            userId: 12,
            name: '陈二',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 5004,
              submitTime: '2023-10-05T17:50:00',
              comment: '完成了基础控制算法，还需要优化路径规划',
              fileUrl: 'https://example.com/files/control-algorithm.zip',
              status: SubmissionStatus.SUBMITTED,
              reviewComment: '',
              reviewTime: ''
            }
          },
          {
            userId: 7,
            name: '周九',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 504,
        title: '测试与优化',
        description: '进行系统测试，优化性能和稳定性',
        members: [
          {
            userId: 11,
            name: '刘一',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 12,
            name: '陈二',
            role: '学生',
            department: '人工智能学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  },
  {
    taskId: 6,
    title: '软件工程实践项目',
    description: '开发一个校园二手交易平台，实践软件工程的完整流程',
    startTime: '2023-08-15T08:00:00',
    endTime: '2023-12-15T18:00:00',
    status: TaskStatus.IN_PROGRESS,
    creator: { userId: 16, name: '郑博士', role: '教师' },
    createTime: '2023-08-01T14:20:00',
    completedSubTasks: 2,
    totalSubTasks: 5,
    subTasks: [
      {
        subTaskId: 601,
        title: '需求分析与规格说明',
        description: '收集用户需求，编写软件需求规格说明书',
        members: [
          {
            userId: 13,
            name: '张明',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 6001,
              submitTime: '2023-08-30T16:20:00',
              comment: '完成了需求调研和分析，形成了初步的需求文档',
              fileUrl: 'https://example.com/files/requirements-spec.docx',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '需求分析全面，用例描述清晰',
              reviewTime: '2023-08-31T10:15:00'
            }
          }
        ]
      },
      {
        subTaskId: 602,
        title: '系统设计',
        description: '完成系统架构设计和详细设计',
        members: [
          {
            userId: 14,
            name: '王丽',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 6002,
              submitTime: '2023-09-20T15:40:00',
              comment: '完成了系统架构设计和数据库设计',
              fileUrl: 'https://example.com/files/system-design.pdf',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '设计文档规范，架构合理',
              reviewTime: '2023-09-21T11:30:00'
            }
          }
        ]
      },
      {
        subTaskId: 603,
        title: '前端开发',
        description: '开发用户界面和前端功能',
        members: [
          {
            userId: 13,
            name: '张明',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 6003,
              submitTime: '2023-10-15T17:30:00',
              comment: '完成了主要页面的开发，包括登录、注册、商品列表和详情页',
              fileUrl: 'https://example.com/files/frontend-code.zip',
              status: SubmissionStatus.SUBMITTED,
              reviewComment: '',
              reviewTime: ''
            }
          },
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 604,
        title: '后端开发',
        description: '开发服务器端API和业务逻辑',
        members: [
          {
            userId: 14,
            name: '王丽',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 6004,
              submitTime: '2023-10-20T16:45:00',
              comment: '完成了用户管理、商品管理和订单管理的API开发',
              fileUrl: 'https://example.com/files/backend-code.zip',
              status: SubmissionStatus.SUBMITTED,
              reviewComment: '',
              reviewTime: ''
            }
          },
          {
            userId: 8,
            name: '吴十',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 605,
        title: '测试与部署',
        description: '进行系统测试，修复bug，部署上线',
        members: [
          {
            userId: 13,
            name: '张明',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 14,
            name: '王丽',
            role: '学生',
            department: '软件工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  },
  {
    taskId: 7,
    title: '数据可视化大屏设计',
    description: '设计并实现一个校园数据可视化大屏，展示学校各类数据指标',
    startTime: '2023-07-10T08:00:00',
    endTime: '2023-08-20T18:00:00',
    status: TaskStatus.COMPLETED,
    creator: { userId: 10, name: '李老师', role: '教师' },
    createTime: '2023-07-01T09:30:00',
    completedSubTasks: 3,
    totalSubTasks: 3,
    subTasks: [
      {
        subTaskId: 701,
        title: '数据收集与处理',
        description: '收集学校各部门数据，进行清洗和整合',
        members: [
          {
            userId: 5,
            name: '钱七',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 7001,
              submitTime: '2023-07-25T15:20:00',
              comment: '完成了数据收集和清洗，建立了统一的数据仓库',
              fileUrl: 'https://example.com/files/data-warehouse.xlsx',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '数据整理得很全面，格式规范',
              reviewTime: '2023-07-26T10:30:00'
            }
          }
        ]
      },
      {
        subTaskId: 702,
        title: '可视化设计',
        description: '设计各类数据图表和大屏布局',
        members: [
          {
            userId: 6,
            name: '孙八',
            role: '学生',
            department: '电子工程学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 7002,
              submitTime: '2023-08-05T16:40:00',
              comment: '完成了所有图表的设计和大屏布局',
              fileUrl: 'https://example.com/files/visualization-design.fig',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '设计美观，信息层次清晰',
              reviewTime: '2023-08-06T11:15:00'
            }
          }
        ]
      },
      {
        subTaskId: 703,
        title: '前端实现与部署',
        description: '使用前端技术实现可视化大屏，并部署到指定服务器',
        members: [
          {
            userId: 7,
            name: '周九',
            role: '学生',
            department: '计算机科学学院',
            avatar: '/images/default-avatar.svg',
            submission: {
              submissionId: 7003,
              submitTime: '2023-08-18T17:30:00',
              comment: '完成了所有图表的实现和大屏的部署',
              fileUrl: 'https://example.com/files/dashboard-code.zip',
              status: SubmissionStatus.REVIEWED,
              reviewComment: '代码质量高，性能优化做得好',
              reviewTime: '2023-08-19T10:45:00'
            }
          }
        ]
      }
    ]
  },
  {
    taskId: 8,
    title: '校园文化活动策划',
    description: '策划并组织一场校园文化活动，包括前期策划、中期执行和后期总结',
    startTime: '2023-09-10T08:00:00',
    endTime: '2023-10-20T18:00:00',
    status: TaskStatus.PENDING_REVIEW,
    creator: { userId: 9, name: '陈老师', role: '教师' },
    createTime: '2023-08-25T14:15:00',
    completedSubTasks: 0,
    totalSubTasks: 3,
    subTasks: [
      {
        subTaskId: 801,
        title: '活动策划',
        description: '确定活动主题，制定活动方案',
        members: [
          {
            userId: 1,
            name: '张三',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 3,
            name: '王五',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 802,
        title: '活动执行',
        description: '按计划执行活动，协调各方资源',
        members: [
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 4,
            name: '赵六',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      },
      {
        subTaskId: 803,
        title: '活动总结',
        description: '收集活动反馈，撰写活动总结报告',
        members: [
          {
            userId: 1,
            name: '张三',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          },
          {
            userId: 2,
            name: '李四',
            role: '学生',
            department: '信息工程学院',
            avatar: '/images/default-avatar.svg',
            submission: null
          }
        ]
      }
    ]
  }
];

// 更新模拟统计数据
export const mockStatistics = {
  totalTasks: 8,
  inProgressTasks: 3,
  completedTasks: 2,
  overdueTasks: 1,
  notStartedTasks: 2,
  completionRate: 25, // 百分比
  // 近7天任务分布
  last7Days: {
    notStarted: 2,
    inProgress: 3,
    completed: 0,
    overdue: 0
  },
  // 近30天任务分布
  last30Days: {
    notStarted: 2,
    inProgress: 3,
    completed: 2,
    overdue: 1
  },
  // 所有任务分布
  all: {
    notStarted: 2,
    inProgress: 3,
    completed: 2,
    overdue: 1
  }
}; 