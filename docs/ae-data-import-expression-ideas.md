# 采集数据导入 Adobe After Effects 与表达式呈现构思

> 这是一份提前构思文档，不影响当前 Mod 第一阶段与第二阶段实现目标。  
> 当前 Mod 仍优先做 Minecraft 内的数据采集、里程累加、出口区域标注。  
> 本文只讨论后续如何把采集到的数据导入 AE，并用表达式或脚本呈现为 HUD、路线信息、出口提示等画面元素。

## 1. 输入数据假设

后续 Mod 可能导出的 JSON 大致包含：

```json
{
  "meta": {
    "minecraftVersion": "1.12.2",
    "sampleRate": "1 tick",
    "timeSource": "System.nanoTime"
  },
  "summary": {
    "sampleCount": 12000,
    "durationSeconds": 600.0,
    "totalDistance2d": 1685.5,
    "totalDistance3d": 1692.8
  },
  "exitRegions": [
    {
      "id": "exit_1685",
      "min": { "x": 1150, "y": 50, "z": -850 },
      "max": { "x": 1250, "y": 90, "z": -750 }
    }
  ],
  "samples": [
    {
      "tick": 12500,
      "timeSeconds": 625.0,
      "position": { "x": 1200.0, "y": 64.0, "z": -800.0 },
      "yaw": 90.0,
      "exitRegionId": "exit_1685",
      "delta": {
        "dx": 0.12,
        "dy": 0.0,
        "dz": 0.05,
        "distance2d": 0.13,
        "distance3d": 0.13
      },
      "odometer": {
        "distance2d": 1685.5,
        "distance3d": 1692.8
      }
    }
  ]
}
```

AE 主要会用这些字段：

- `timeSeconds`：把 MC 样本映射到 AE 时间轴。
- `position.x/y/z`：位置、地图、路线、调试显示。
- `yaw`：指南针、朝向箭头、画面方向。
- `odometer.distance2d`：行驶里程、剩余里程计算基础。
- `exitRegionId`：出口提示、路牌弹出、时间轴事件。

## 2. 导入 AE 的几种方案

### 2.1 方案 A：JSX 脚本读取 JSON 并生成关键帧

这是最稳的方案。

流程：

```text
1. Mod 导出 capture.json。
2. 用户在 AE 中运行 import_capture.jsx。
3. JSX 读取 JSON。
4. JSX 创建控制层和文字层。
5. JSX 把关键数据写成 AE 关键帧。
6. AE 表达式只负责读取图层属性并格式化显示。
```

适合：

- 数据量中等或较大。
- 需要稳定渲染。
- 不希望 AE 表达式每帧解析大 JSON。

优点：

- 渲染稳定。
- 表达式简单。
- 可把数据转成 AE 原生关键帧。
- 可以自动生成 marker、文字层、控制层。

缺点：

- 每次数据更新后需要重新运行脚本。
- JSX 脚本需要维护。

推荐程度：高。

### 2.2 方案 B：AE 表达式直接读取 JSON Footage

AE 可以把 JSON 当作 footage 导入，然后表达式读取数据。

流程：

```text
1. 导入 capture.json。
2. AE 表达式通过 footage 读取 JSON。
3. 根据当前 time 查找最近 sample。
4. 显示速度、里程、出口 ID 等。
```

适合：

- 快速测试。
- 数据量较小。
- 想少写 JSX。

优点：

- 思路直接。
- 改 JSON 后可能更容易重新链接。
- 可以让表达式完全驱动画面。

缺点：

- 大 JSON 下表达式性能可能差。
- 每个图层表达式重复查找 sample 会变慢。
- AE 版本兼容性需要注意。

推荐程度：中。

### 2.3 方案 C：预处理为 CSV，再导入 AE

把 JSON 转成更扁平的 CSV：

```csv
timeSeconds,x,y,z,yaw,distance2d,distance3d,exitRegionId
0.000,1000.0,64.0,-500.0,90.0,0.0,0.0,
0.050,1000.2,64.0,-500.1,91.0,0.22,0.22,
```

适合：

- 表格检查。
- 数据分析。
- 后续给其他软件使用。

优点：

- 简单直观。
- 容易检查。
- 可用 Excel 或脚本预处理。

缺点：

- 层级信息弱。
- 出口区域、事件、元数据表达不如 JSON 清晰。

推荐程度：中低。可以作为辅助格式。

### 2.4 方案 D：生成 AE 专用数据脚本

Mod 或外部工具直接生成 `.jsx`，里面包含数据数组和图层创建逻辑。

适合：

- 希望用户双击/运行一个脚本就完成导入。
- 不想让 AE 再处理外部 JSON 路径。

优点：

- 一份 JSX 包含全部数据和逻辑。
- 不容易丢失 JSON 链接。

缺点：

- 数据量大时 JSX 文件会很大。
- 数据和逻辑混在一起，不利于调试。

推荐程度：中。

## 3. 推荐路线

建议后续采用：

```text
Mod 导出 capture.json
  -> 外部或内置转换器生成 import_capture.jsx
  -> AE 运行 JSX
  -> JSX 创建控制层/关键帧/marker
  -> AE 表达式负责格式化和显示
```

也就是：

**JSX 负责导入和写关键帧，AE 表达式负责呈现。**

这样比较稳，不会让 AE 表达式承担太多数据解析压力。

## 4. AE 中的数据组织方式

### 4.1 Data Controller 控制层

可以在 AE 中创建一个 Null 图层：

```text
Data_Controller
```

在它上面放一些 Slider Control 或 Angle Control：

- `Odometer 2D`
- `Odometer 3D`
- `Yaw`
- `Speed`
- `Exit Active`
- `Exit Index`

JSX 把这些属性写成关键帧。

其他文字层或图形层只需要表达式读取控制层：

```javascript
ctrl = thisComp.layer("Data_Controller");
v = ctrl.effect("Odometer 2D")("Slider");
Math.round(v) + " blocks";
```

### 4.2 文字层

可以自动生成：

- `Text_Odometer`
- `Text_Yaw`
- `Text_Exit`
- `Text_Speed`

表达式示例：

```javascript
ctrl = thisComp.layer("Data_Controller");
dist = ctrl.effect("Odometer 2D")("Slider");
Math.round(dist) + " blocks";
```

### 4.3 朝向指针

给箭头图层 Rotation 写表达式：

```javascript
ctrl = thisComp.layer("Data_Controller");
yaw = ctrl.effect("Yaw")("Angle");
-yaw;
```

是否取负号取决于 AE 画面坐标和 Minecraft yaw 的方向约定，后续需要实测。

### 4.4 出口提示

如果 sample 中有 `exitRegionId`，JSX 可以做两件事：

1. 在时间轴上添加 marker。
2. 给 `Exit Active` 写 0/1 关键帧。

文字层表达式：

```javascript
ctrl = thisComp.layer("Data_Controller");
active = ctrl.effect("Exit Active")("Slider");
active > 0.5 ? "出口区域" : "";
```

如果要显示具体 ID，可以用 JSX 直接在对应时间生成文字层或 marker comment。

## 5. 时间映射

因为 Mod 的 `timeSeconds` 来自 `System.nanoTime()`，AE 可以直接使用它作为时间轴秒数。

示例：

```text
sample.timeSeconds = 12.35
AE keyframe time = 12.35
```

如果 AE 合成不是从 0 秒开始，可以加 offset：

```text
aeTime = sample.timeSeconds + compStartOffset
```

如果视频素材和采集开始有偏差，需要一个手动同步偏移：

```text
aeTime = sample.timeSeconds + syncOffset
```

后续可以在 AE 脚本中提供：

```text
syncOffsetSeconds
```

## 6. 插值策略

Minecraft 采样通常是每 tick 一次，约 20Hz。AE 可能是 30fps 或 60fps。

如果 JSX 把每个样本写成关键帧，AE 会自动在关键帧之间插值。

建议：

- 里程 `distance2d`：线性插值。
- yaw：需要注意 359 到 0 的跨角度问题。
- exitRegionId：不要插值，用 0/1 开关。
- 速度：可以提前平滑，也可以 AE 中平滑。

### 6.1 yaw 跨 360 问题

如果 yaw 从 359 跳到 0，AE 可能认为反向转了 359 度。

解决方式：

- 导入前对 yaw 做 unwrap。
- 或者只把 yaw 用于文本显示，不直接做连续旋转。
- 或在 JSX 写关键帧时处理角度连续性。

## 7. 出口区域在 AE 中的呈现

第二阶段样本有 `exitRegionId` 后，AE 可以做：

- 进入出口区域时弹出出口提示。
- 当前出口 ID 显示为文字。
- 时间轴 marker 标注出口。
- 图层颜色变化。
- 距离面板切换状态。

可以从 samples 中提取连续区域：

```text
exitRegionId 从 null 变成 exit_1685 -> enter
exitRegionId 从 exit_1685 变成 null -> leave
```

生成事件：

```json
{
  "type": "exit_enter",
  "id": "exit_1685",
  "timeSeconds": 125.0
}
```

AE 中可对应：

- marker at 125.0s
- Exit Active = 1
- Exit Name = exit_1685

## 8. 数据规模与性能

假设录制 10 分钟：

```text
20 samples/s * 600s = 12000 samples
```

这对 JSON 来说不大，但如果每个图层表达式都遍历 12000 个 samples，会变慢。

因此推荐：

- 不让每个表达式自己遍历 JSON。
- 用 JSX 预先写关键帧。
- 表达式只读取当前图层属性或控制层属性。

这也是推荐 “JSX 导入 + 表达式呈现” 的主要原因。

## 9. 可行的后续产物

后续可以做几个文件：

```text
capture.json
```

Mod 输出的原始数据。

```text
import_capture.jsx
```

AE 导入脚本，读取 JSON，创建控制层和关键帧。

```text
hud_template.aep
```

AE 模板工程，里面已经有 HUD 图层和表达式。

```text
capture_events.json
```

可选，预处理后得到的出口进入/离开事件。

## 10. 当前建议结论

后续最稳的 AE 路线是：

**不要让 AE 表达式直接承担完整数据解析，而是用 JSX 把采集数据转换成 AE 原生关键帧；表达式只负责把关键帧数值显示成 HUD。**

推荐流程：

```text
capture.json
  -> import_capture.jsx
  -> Data_Controller 关键帧
  -> AE 表达式读取控制层
  -> HUD/出口提示/里程/朝向显示
```

这样既保留 JSON 的清晰结构，也能让 AE 合成和渲染更稳定。
