// AGV显示功能测试脚本
console.log('=== AGV显示功能测试 ===');

// 模拟测试数据
const testData = [
  {
    id: 1,
    agvId: 'AGV_001',
    ipAddress: '127.0.0.1',
    port: 5555,
    status: 'connected',
    currentLocation: 'A1'
  },
  {
    id: 2,
    agvId: 'AGV_002',
    ipAddress: '127.0.0.1',
    port: 2233,
    status: 'disconnected',
    currentLocation: 'B2'
  }
];

console.log('测试数据:', testData);

// 模拟前端处理逻辑
function processAgvData(agvList) {
  return agvList.map(agv => ({
    ...agv,
    status: agv.status || 'disconnected',
    ip: agv.ipAddress || '',
    port: agv.port || 0
  }));
}

const processedData = processAgvData(testData);
console.log('处理后的数据:', processedData);

// 验证显示逻辑
console.log('\n=== 显示验证 ===');
processedData.forEach(agv => {
  console.log(`AGV ${agv.agvId}:`);
  console.log(`  - 状态: ${agv.status}`);
  console.log(`  - IP: ${agv.ip}`);
  console.log(`  - 端口: ${agv.port}`);
  console.log(`  - 位置: ${agv.currentLocation}`);
});

console.log('\n=== 测试完成 ===');