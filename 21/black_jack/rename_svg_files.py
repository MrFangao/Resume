#!/usr/bin/env python3
"""
批量重命名SVG文件，从 {rank}{suit}.svg 格式改为 {suit}{rank}.svg 格式
例如：2C.svg -> C2.svg, AH.svg -> HA.svg, TS.svg -> ST.svg
"""

import os
from pathlib import Path

# 花色映射（保持大写）
SUIT_MAP = {
    'C': 'C',  # Clubs
    'D': 'D',  # Diamonds
    'H': 'H',  # Hearts
    'S': 'S',  # Spades
}

def rename_svg_file(old_name):
    """
    将文件名从 {rank}{suit}.svg 转换为 {suit}{rank}.svg
    
    示例：
    - 2C.svg -> C2.svg
    - AH.svg -> HA.svg
    - TS.svg -> ST.svg (T代表10)
    - JC.svg -> CJ.svg
    """
    if old_name == 'CoverCard.svg':
        return old_name  # 不重命名CoverCard
    
    if not old_name.endswith('.svg'):
        return old_name
    
    # 去掉扩展名
    base_name = old_name[:-4]
    
    # 处理特殊情况：10（已经用T表示）
    if base_name == 'TS':
        return 'ST.svg'
    elif base_name == 'TD':
        return 'DT.svg'
    elif base_name == 'TH':
        return 'HT.svg'
    elif base_name == 'TC':
        return 'CT.svg'
    
    # 处理字母牌：A, J, Q, K
    if len(base_name) == 2:
        first_char = base_name[0]
        second_char = base_name[1]
        
        # 如果第一个字符是花色（C, D, H, S），第二个是点数，已经是正确格式
        if first_char in SUIT_MAP and second_char not in SUIT_MAP:
            return old_name  # 已经是正确格式
        
        # 如果第一个字符是点数（数字或A,J,Q,K），第二个是花色
        if second_char in SUIT_MAP:
            return f"{second_char}{first_char}.svg"
    
    # 如果无法识别，保持原样
    return old_name

def main():
    assets_dir = Path(__file__).parent / 'assets' / 'Poker_card'
    
    if not assets_dir.exists():
        print(f"错误：找不到目录 {assets_dir}")
        return
    
    # 获取所有SVG文件
    svg_files = list(assets_dir.glob('*.svg'))
    
    if not svg_files:
        print("未找到SVG文件")
        return
    
    # 创建重命名映射
    rename_map = {}
    for svg_file in svg_files:
        old_name = svg_file.name
        new_name = rename_svg_file(old_name)
        
        if old_name != new_name:
            rename_map[old_name] = new_name
    
    if not rename_map:
        print("所有文件已经是对应的格式，无需重命名")
        return
    
    # 显示重命名计划
    print("准备重命名以下文件：")
    for old_name, new_name in sorted(rename_map.items()):
        print(f"  {old_name} -> {new_name}")
    
    # 执行重命名
    print(f"\n开始重命名 {len(rename_map)} 个文件...")
    for old_name, new_name in rename_map.items():
        old_path = assets_dir / old_name
        new_path = assets_dir / new_name
        
        # 检查目标文件是否已存在
        if new_path.exists():
            print(f"警告：{new_name} 已存在，跳过 {old_name}")
            continue
        
        try:
            old_path.rename(new_path)
            print(f"✓ {old_name} -> {new_name}")
        except Exception as e:
            print(f"✗ 重命名 {old_name} 失败: {e}")
    
    print("\n重命名完成！")

if __name__ == '__main__':
    main()

