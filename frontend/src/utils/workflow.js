export const categoryName = value => ({ PHYSICS: '物理类', HISTORY: '历史类' }[value] || value)
export const subjectName = value => ({ CHEMISTRY: '化学', BIOLOGY: '生物', POLITICS: '政治', GEOGRAPHY: '地理' }[value] || value)
export const resultName = value => ({ FILED: '已投档', SLIPPED: '滑档', BELOW_CONTROL_LINE: '低于控制线', NO_VALID_PREFERENCE: '无有效志愿' }[value] || value)
export const formatTime = value => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '未设置'
