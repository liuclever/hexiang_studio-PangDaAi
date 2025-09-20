/**
 * 格式化日期时间
 * @param dateTime 日期时间
 * @param format 格式，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期时间字符串
 */
export function formatDateTime(dateTime: string | Date | number | null | undefined, format: string = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!dateTime) return '--';
  
  const date = new Date(dateTime);
  
  if (isNaN(date.getTime())) {
    console.warn('Invalid date:', dateTime);
    return '--';
  }
  
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours();
  const minutes = date.getMinutes();
  const seconds = date.getSeconds();
  
  return format
    .replace(/YYYY/g, year.toString())
    .replace(/MM/g, month < 10 ? `0${month}` : month.toString())
    .replace(/DD/g, day < 10 ? `0${day}` : day.toString())
    .replace(/HH/g, hours < 10 ? `0${hours}` : hours.toString())
    .replace(/mm/g, minutes < 10 ? `0${minutes}` : minutes.toString())
    .replace(/ss/g, seconds < 10 ? `0${seconds}` : seconds.toString());
}

/**
 * 格式化日期
 * @param date 日期
 * @param format 格式，默认为 'YYYY-MM-DD'
 * @returns 格式化后的日期字符串
 */
export function formatDate(date: string | Date | number | null | undefined, format: string = 'YYYY-MM-DD'): string {
  return formatDateTime(date, format);
}

/**
 * 格式化时间
 * @param time 时间
 * @param format 格式，默认为 'HH:mm:ss'
 * @returns 格式化后的时间字符串
 */
export function formatTime(time: string | Date | number | null | undefined, format: string = 'HH:mm:ss'): string {
  return formatDateTime(time, format);
}

/**
 * 获取当前日期时间
 * @param format 格式，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 当前日期时间字符串
 */
export function getCurrentDateTime(format: string = 'YYYY-MM-DD HH:mm:ss'): string {
  return formatDateTime(new Date(), format);
}

/**
 * 获取当前日期
 * @param format 格式，默认为 'YYYY-MM-DD'
 * @returns 当前日期字符串
 */
export function getCurrentDate(format: string = 'YYYY-MM-DD'): string {
  return formatDate(new Date(), format);
}

/**
 * 获取当前时间
 * @param format 格式，默认为 'HH:mm:ss'
 * @returns 当前时间字符串
 */
export function getCurrentTime(format: string = 'HH:mm:ss'): string {
  return formatTime(new Date(), format);
}

/**
 * 日期时间比较
 * @param date1 日期时间1
 * @param date2 日期时间2
 * @returns 0: 相等, 1: date1>date2, -1: date1<date2
 */
export function compareDateTime(date1: string | Date | number | null | undefined, date2: string | Date | number | null | undefined): number {
  const d1 = new Date(date1 || 0).getTime();
  const d2 = new Date(date2 || 0).getTime();
  
  if (d1 === d2) return 0;
  return d1 > d2 ? 1 : -1;
}

/**
 * 计算日期差（天数）
 * @param date1 日期1
 * @param date2 日期2
 * @returns 相差的天数
 */
export function dateDiff(date1: string | Date | number | null | undefined, date2: string | Date | number | null | undefined): number {
  const d1 = new Date(date1 || 0);
  const d2 = new Date(date2 || 0);
  
  // 转换为相同的时间部分
  d1.setHours(0, 0, 0, 0);
  d2.setHours(0, 0, 0, 0);
  
  // 计算差值
  return Math.floor((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
} 