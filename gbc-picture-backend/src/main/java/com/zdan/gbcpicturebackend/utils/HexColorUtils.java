package com.zdan.gbcpicturebackend.utils;

/**
 * 解决数据万象省略前导零的情况
 * <p>
 * 算法思想：
 * 首先我们应该先摘下0x这个前缀，然后对剩余的十六进制位单独处理，
 * r，g，b三个两位十六进制互相独立，我们分成三组依次处理，判断截取后的字符串长度，
 * 如果长度为3，直接返回0x0r0g0b，
 * 如果长度为4位或者5位，我们接着讨论；
 * ① 开头为0，那么第一组的结果就为00，
 * ② 开头不为0，那么第一组的结果应该为第一个十六进制为拼接上第二个十六进制位，
 * ③ 其他两组也是如此。
 * 如果长度为6位，则拼接上0x直接返回。
 */
public class HexColorUtils {


    /**
     * 将压缩的十六进制颜色转换为标准格式
     *
     * @param compressedHexColor 压缩的十六进制颜色（如 "000"）
     * @return 标准格式的十六进制颜色（如 "0x000000"）
     */
    public static String toStandardHexColor(String compressedHexColor) {
        // 去除可能存在的0x前缀
        String hexStr = compressedHexColor.startsWith("0x") ? compressedHexColor.substring(2) : compressedHexColor;
        int length = hexStr.length();
        // 没有省略前导零，不需要操作直接返回
        if (length == 6) {
            return "0x" + hexStr;
        }
        // 长度为3时候，每个颜色分量都省略了前导零
        if (length == 3) {
            String[] split = hexStr.split("");
            StringBuilder stringBuilder = new StringBuilder("0x");
            for (String s : split) {
                stringBuilder.append("0").append(s);
            }
            return stringBuilder.toString();
        }
        // 输入字符串的索引
        int hexStrIndex = 0;
        StringBuilder stringBuilder = new StringBuilder();
        // 长度为4或5的情况
        for (int i = 0; i < 3; i++) { // i -- RGB分量的索引
            char current = hexStr.charAt(hexStrIndex);
            if (current == '0') {
                // 当前分量是00的情况
                stringBuilder.append("00");
                hexStrIndex++;
            } else {
                // 正常分量处理（可能包含补零）
                if (hexStrIndex + 1 < length) {
                    stringBuilder.append(current).append(hexStr.charAt(hexStrIndex + 1));
                    hexStrIndex += 2;
                } else {
                    // 最后一个字符单独处理，补零
                    stringBuilder.append(current).append('0');
                    hexStrIndex += 2;
                }
            }
        }

        return "0x" + stringBuilder.toString();
    }

    public static void main(String[] args) {
        // 测试用例
        System.out.println(toStandardHexColor("000"));     // 0x000000
        System.out.println(toStandardHexColor("0a00"));    // 0x00a000
        System.out.println(toStandardHexColor("a0b40"));   // 0xa0b400
        System.out.println(toStandardHexColor("0ab0"));    // 0x00ab00
        System.out.println(toStandardHexColor("00ab"));   // 0x0000ab
        System.out.println(toStandardHexColor("0ab00"));  // 0x00ab00
        System.out.println(toStandardHexColor("00a0"));  // 0x00ab00
    }
}