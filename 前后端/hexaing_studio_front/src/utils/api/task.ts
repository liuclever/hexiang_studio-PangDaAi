import request from '../request';
import { mockTasks, mockStatistics } from './mockData/taskMockData';

// 使用模拟数据的标志
const USE_MOCK = false;

// 主任务相关接口
export const getTasks = (params: any) => {
  if (USE_MOCK) {
    // 模拟分页和搜索过滤
    return new Promise((resolve) => {
      setTimeout(() => {
        let filteredTasks = [...mockTasks];
        
        // 关键字过滤
        if (params.keyword) {
          const keyword = params.keyword.toLowerCase();
          filteredTasks = filteredTasks.filter(task => 
            task.title.toLowerCase().includes(keyword) || 
            task.description.toLowerCase().includes(keyword)
          );
        }
        
        // 状态过滤
        if (params.status) {
          filteredTasks = filteredTasks.filter(task => task.status === params.status);
        }
        
        // 日期过滤 - 使用startTime/endTime字段
        if (params.startTime) {
          const startTime = new Date(params.startTime);
          filteredTasks = filteredTasks.filter(task => {
            const taskDate = new Date(task.startTime);
            return taskDate >= startTime;
          });
        }
        
        if (params.endTime) {
          const endTime = new Date(params.endTime);
          filteredTasks = filteredTasks.filter(task => {
            const taskDate = new Date(task.endTime);
            return taskDate <= endTime;
          });
        }
        
        // 分页处理
        const page = params.page || 1;
        const size = params.size || 10;
        const start = (page - 1) * size;
        const end = start + size;
        const paginatedTasks = filteredTasks.slice(start, end);
        
        resolve({
          code: 200,
          message: 'success',
          data: {
            records: paginatedTasks,
            total: filteredTasks.length,
            pages: Math.ceil(filteredTasks.length / size),
            page: page,           // 使用page而不是current
            pageSize: size,       // 使用pageSize而不是size
            hasNext: page < Math.ceil(filteredTasks.length / size),
            hasPrevious: page > 1
          }
        });
      }, 300);
    });
  }
  
  return request({
    url: '/admin/task/tasks',  // 修改为正确的API路径
    method: 'get',
    params
  });
};

export const getTaskById = (taskId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const task = mockTasks.find(t => t.taskId == taskId);
        
        if (task) {
          // 处理字段名不一致的问题
          const responseTask = {
            ...task,
            // 使用统一的时间字段名
            startTime: task.startTime,
            endTime: task.endTime,
            // 从子任务中收集成员ID
            memberIds: collectMemberIdsFromSubTasks(task)
          };
          
          resolve({
            code: 200,
            message: 'success',
            data: responseTask
          });
        } else {
          resolve({
            code: 404,
            message: '任务不存在',
            data: null
          });
        }
      }, 300);
    });
  }
  
  return request({
    url: `/admin/task/${taskId}`,
    method: 'get'
  });
};

// 从子任务中收集所有成员ID
const collectMemberIdsFromSubTasks = (task: any) => {
  if (!task.subTasks || !Array.isArray(task.subTasks)) {
    return [];
  }
  
  const memberIds = new Set<number>();
  
  task.subTasks.forEach((subTask: any) => {
    if (subTask.members && Array.isArray(subTask.members)) {
      subTask.members.forEach((member: any) => {
        if (member.userId) {
          memberIds.add(Number(member.userId));
        }
      });
    }
  });
  
  return Array.from(memberIds);
};

/**
 * 添加任务
 * @param data FormData a
 */
export const addTask = (data: FormData) => {
  return request({
    url: '/admin/task/add',
    method: 'post',
    data
  })
}

/**
 * 更新任务
 * @param data FormData
 */
export const updateTask = (data: FormData) => {
  return request({
    url: '/admin/task/update',
    method: 'put',
    data
  })
}

export const deleteTask = (taskId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const taskIndex = mockTasks.findIndex(t => t.taskId == taskId);
        
        if (taskIndex >= 0) {
          // 在实际应用中，这里会从数组中删除任务
          // mockTasks.splice(taskIndex, 1);
          
          resolve({
            code: 200,
            message: '删除任务成功',
            data: null
          });
        } else {
          resolve({
            code: 404,
            message: '任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/admin/task/${taskId}`,
    method: 'delete'
  });
};

// 子任务相关接口
export const getSubTasks = (taskId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const task = mockTasks.find(t => t.taskId == taskId);
        
        if (task) {
          resolve({
            code: 200,
            message: 'success',
            data: task.subTasks || []
          });
        } else {
          resolve({
            code: 404,
            message: '任务不存在',
            data: []
          });
        }
      }, 300);
    });
  }
  
  return request({
    url: `/api/tasks/${taskId}/subtasks`,
    method: 'get'
  });
};

export const createSubTask = (taskId: number | string, data: any) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const task = mockTasks.find(t => t.taskId == taskId);
        
        if (task) {
          // 生成新的子任务ID
          let maxSubTaskId = 0;
          mockTasks.forEach(t => {
            t.subTasks.forEach(st => {
              if (Number(st.subTaskId) > maxSubTaskId) {
                maxSubTaskId = Number(st.subTaskId);
              }
            });
          });
          
          const newSubTask = {
            ...data,
            subTaskId: maxSubTaskId + 1,
            members: data.members || []
          };
          
          // 在实际应用中，这里会将新子任务添加到数组中
          // task.subTasks.push(newSubTask);
          // task.totalSubTasks += 1;
          
          resolve({
            code: 200,
            message: '创建子任务成功',
            data: newSubTask
          });
        } else {
          resolve({
            code: 404,
            message: '任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/tasks/${taskId}/subtasks`,
    method: 'post',
    data
  });
};

export const updateSubTask = (subtaskId: number | string, data: any) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        let updatedSubTask = null;
        
        mockTasks.forEach(task => {
          const subTaskIndex = task.subTasks.findIndex(st => st.subTaskId == subtaskId);
          if (subTaskIndex >= 0) {
            found = true;
            // 在实际应用中，这里会更新子任务
            // const updated = { ...task.subTasks[subTaskIndex], ...data };
            // task.subTasks[subTaskIndex] = updated;
            updatedSubTask = { ...task.subTasks[subTaskIndex], ...data };
          }
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '更新子任务成功',
            data: updatedSubTask
          });
        } else {
          resolve({
            code: 404,
            message: '子任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}`,
    method: 'put',
    data
  });
};

export const deleteSubTask = (subtaskId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        
        mockTasks.forEach(task => {
          const subTaskIndex = task.subTasks.findIndex(st => st.subTaskId == subtaskId);
          if (subTaskIndex >= 0) {
            found = true;
            // 在实际应用中，这里会删除子任务
            // task.subTasks.splice(subTaskIndex, 1);
            // task.totalSubTasks -= 1;
          }
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '删除子任务成功',
            data: null
          });
        } else {
          resolve({
            code: 404,
            message: '子任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}`,
    method: 'delete'
  });
};

// 子任务成员相关接口
export const getSubTaskMembers = (subtaskId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let members: any[] = [];
        
        mockTasks.forEach(task => {
          const subTask = task.subTasks.find(st => st.subTaskId == subtaskId);
          if (subTask) {
            members = subTask.members;
          }
        });
        
        resolve({
          code: 200,
          message: 'success',
          data: members
        });
      }, 300);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}/members`,
    method: 'get'
  });
};

export const addSubTaskMember = (subtaskId: number | string, data: any) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        
        mockTasks.forEach(task => {
          const subTask = task.subTasks.find(st => st.subTaskId == subtaskId);
          if (subTask) {
            found = true;
            // 在实际应用中，这里会添加成员
            // subTask.members.push({
            //   ...data,
            //   submission: null
            // });
          }
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '添加成员成功',
            data: { ...data, submission: null }
          });
        } else {
          resolve({
            code: 404,
            message: '子任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}/members`,
    method: 'post',
    data
  });
};

export const removeSubTaskMember = (subtaskId: number | string, memberId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        
        mockTasks.forEach(task => {
          const subTask = task.subTasks.find(st => st.subTaskId == subtaskId);
          if (subTask) {
            const memberIndex = subTask.members.findIndex(m => m.userId == memberId);
            if (memberIndex >= 0) {
              found = true;
              // 在实际应用中，这里会删除成员
              // subTask.members.splice(memberIndex, 1);
            }
          }
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '移除成员成功',
            data: null
          });
        } else {
          resolve({
            code: 404,
            message: '成员不存在或子任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}/members/${memberId}`,
    method: 'delete'
  });
};

// 提交与审核相关接口
export const submitSubTask = (subtaskId: number | string, memberId: number | string, data: any) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        
        mockTasks.forEach(task => {
          const subTask = task.subTasks.find(st => st.subTaskId == subtaskId);
          if (subTask) {
            const member = subTask.members.find(m => m.userId == memberId);
            if (member) {
              found = true;
              // 在实际应用中，这里会更新成员的提交
              // member.submission = {
              //   submissionId: Date.now(),
              //   submitTime: new Date().toISOString(),
              //   comment: data.comment || '',
              //   fileUrl: data.fileUrl || '',
              //   status: 'SUBMITTED',
              //   reviewComment: '',
              //   reviewTime: ''
              // };
            }
          }
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '提交成功',
            data: {
              submissionId: Date.now(),
              submitTime: new Date().toISOString(),
              comment: data.comment || '',
              fileUrl: data.fileUrl || '',
              status: 'SUBMITTED',
              reviewComment: '',
              reviewTime: ''
            }
          });
        } else {
          resolve({
            code: 404,
            message: '成员不存在或子任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}/members/${memberId}/submit`,
    method: 'post',
    data
  });
};

export const reviewSubmission = (submissionId: number | string, data: any) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let found = false;
        
        mockTasks.forEach(task => {
          task.subTasks.forEach(subTask => {
            subTask.members.forEach(member => {
              if (member.submission && member.submission.submissionId == submissionId) {
                found = true;
                // 在实际应用中，这里会更新提交状态
                // member.submission.status = data.status;
                // member.submission.reviewComment = data.comment || '';
                // member.submission.reviewTime = new Date().toISOString();
                
                // 如果审核通过，更新任务完成计数
                // if (data.status === 'REVIEWED' && member.submission.status !== 'REVIEWED') {
                //   task.completedSubTasks += 1;
                // }
                // 如果取消通过，减少任务完成计数
                // else if (data.status !== 'REVIEWED' && member.submission.status === 'REVIEWED') {
                //   task.completedSubTasks -= 1;
                // }
              }
            });
          });
        });
        
        if (found) {
          resolve({
            code: 200,
            message: '审核成功',
            data: {
              status: data.status,
              reviewComment: data.comment || '',
              reviewTime: new Date().toISOString()
            }
          });
        } else {
          resolve({
            code: 404,
            message: '提交记录不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/admin/task/submission/review/${submissionId}`,
    method: 'post',
    data
  });
};

export const getSubmission = (subtaskId: number | string, memberId: number | string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let submission = null;
        
        mockTasks.forEach(task => {
          const subTask = task.subTasks.find(st => st.subTaskId == subtaskId);
          if (subTask) {
            const member = subTask.members.find(m => m.userId == memberId);
            if (member) {
              submission = member.submission;
            }
          }
        });
        
        resolve({
          code: 200,
          message: 'success',
          data: submission
        });
      }, 300);
    });
  }
  
  return request({
    url: `/api/subtasks/${subtaskId}/members/${memberId}/submission`,
    method: 'get'
  });
};

// 任务统计相关接口
export const getTaskStatistics = () => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: 'success',
          data: mockStatistics
        });
      }, 300);
    });
  }
  
  return request({
    url: '/admin/task/statistics',
    method: 'get'
  });
};

export const updateTaskStatus = (taskId: number | string, status: string, newEndTime?: string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const taskIndex = mockTasks.findIndex(t => t.taskId == taskId);
        
        if (taskIndex >= 0) {
          // 更新任务状态
          const updatedTask = { ...mockTasks[taskIndex], status };
          
          // 如果提供了新的截止时间，也一并更新
          if (newEndTime) {
            updatedTask.endTime = newEndTime;
          }
          
          // 在实际应用中，这里会更新数组中的任务
          mockTasks[taskIndex] = updatedTask;
          
          resolve({
            code: 200,
            message: '更新任务状态成功',
            data: updatedTask
          });
        } else {
          resolve({
            code: 404,
            message: '任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  const data = { status };
  if (newEndTime) {
    Object.assign(data, { endTime: newEndTime });
  }
  
  return request({
    url: '/admin/task/updateStatus',
    method: 'put',
    data: { taskId, status, newEndTime }
  });
};

// 任务审批相关接口
export const getPendingApprovalTasks = (params: any) => {
  if (USE_MOCK) {
    // 模拟待审批任务数据
    return new Promise((resolve) => {
      setTimeout(() => {
        const pendingTasks = mockTasks.filter(task => task.status === 'PENDING_REVIEW');
        
        let filteredTasks = [...pendingTasks];
        
        // 关键字过滤
        if (params.title) {
          const keyword = params.title.toLowerCase();
          filteredTasks = filteredTasks.filter(task => 
            task.title.toLowerCase().includes(keyword) || 
            task.description.toLowerCase().includes(keyword)
          );
        }
        
        // 分页处理
        const page = params.page || 1;
        const pageSize = params.pageSize || 20;
        const start = (page - 1) * pageSize;
        const end = start + pageSize;
        const paginatedTasks = filteredTasks.slice(start, end);
        
        resolve({
          code: 1,
          message: 'success',
          data: {
            records: paginatedTasks,
            total: filteredTasks.length,
            pages: Math.ceil(filteredTasks.length / pageSize),
            current: page,
            size: pageSize
          }
        });
      }, 300);
    });
  }
  
  return request({
    url: '/admin/task/pending-approval',
    method: 'get',
    params
  });
};

export const approveTask = (taskId: number | string, comment?: string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const taskIndex = mockTasks.findIndex(t => t.taskId == taskId);
        
        if (taskIndex >= 0) {
          // 更新任务状态为已完成
          mockTasks[taskIndex].status = 'COMPLETED';
          
          resolve({
            code: 1,
            message: '任务审批成功',
            data: null
          });
        } else {
          resolve({
            code: 0,
            message: '任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/admin/task/approve/${taskId}`,
    method: 'post',
    params: { approvalComment: comment }
  });
};

export const rejectTask = (taskId: number | string, reason: string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const taskIndex = mockTasks.findIndex(t => t.taskId == taskId);
        
        if (taskIndex >= 0) {
          // 更新任务状态为被退回
          mockTasks[taskIndex].status = 'REJECTED';
          
          resolve({
            code: 1,
            message: '任务已退回',
            data: null
          });
        } else {
          resolve({
            code: 0,
            message: '任务不存在',
            data: null
          });
        }
      }, 500);
    });
  }
  
  return request({
    url: `/admin/task/reject/${taskId}`,
    method: 'post',
    params: { rejectionReason: reason }
  });
};

export const batchApproveTask = (taskIds: number[], comment?: string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        let successCount = 0;
        
        taskIds.forEach(taskId => {
          const taskIndex = mockTasks.findIndex(t => t.taskId == taskId);
          if (taskIndex >= 0) {
            mockTasks[taskIndex].status = 'COMPLETED';
            successCount++;
          }
        });
        
        resolve({
          code: 1,
          message: `批量审批完成，共处理 ${successCount} 个任务`,
          data: { successCount, total: taskIds.length }
        });
      }, 1000);
    });
  }
  
  return request({
    url: '/admin/task/batch-approve',
    method: 'post',
    params: { 
      taskIds: taskIds,
      approvalComment: comment 
    }
  });
};

// 获取任务提交审核信息
export const getTaskSubmissions = (taskId: number | string) => {
  return request({
    url: `/admin/task/${taskId}/submissions`,
    method: 'get'
  });
}; 