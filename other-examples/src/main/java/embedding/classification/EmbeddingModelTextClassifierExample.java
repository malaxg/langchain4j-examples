package embedding.classification;

import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.TextClassifier;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static embedding.classification.EmbeddingModelTextClassifierExample.CustomerServiceCategory.*;
import static java.util.Arrays.asList;

/**
 * 演示如何使用基于 Embedding 模型的文本分类器（EmbeddingModelTextClassifier）。
 * <p>
 * 原理：为每个类别准备若干"示例文本"（few-shot 样本），
 * 用 EmbeddingModel 把示例与待分类文本都转成向量，再通过相似度把文本归类。
 * 这里用本地的 all-MiniLM-L6-v2 模型离线完成文本分类。
 */
public class EmbeddingModelTextClassifierExample {

    // 客服工单的类别枚举（用于给用户的提问归类）
    enum CustomerServiceCategory {

        BILLING_AND_PAYMENTS,    // 账单与支付
        TECHNICAL_SUPPORT,       // 技术支持
        ACCOUNT_MANAGEMENT,      // 账号管理
        PRODUCT_INFORMATION,     // 产品信息
        ORDER_STATUS,            // 订单状态
        RETURNS_AND_EXCHANGES,   // 退货与换货
        FEEDBACK_AND_COMPLAINTS  // 反馈与投诉
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：准备每个类别对应的示例文本 → 创建 EmbeddingModel →
     * 用 EmbeddingModelTextClassifier 进行分类 → 打印分类结果。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        Map<CustomerServiceCategory, List<String>> examples = new HashMap<>();
        examples.put(BILLING_AND_PAYMENTS, asList(
                "我可以用 PayPal 付款吗？",
                "你们接受比特币吗？",
                "可以通过电汇付款吗？",
                "我尝试付款时一直报错。",
                "我的银行卡被扣了两次款，你能帮忙处理吗？",
                "为什么我的付款被拒绝了？",
                "我该如何申请退款？",
                "我什么时候能收到退款？",
                "如果我取消订阅，能拿到退款吗？",
                "你能把我上一笔订单的发票发给我吗？",
                "我没有收到购买的收据。",
                "发票会自动发送到我的邮箱吗？",
                "我该如何升级我的订阅？",
                "基础版和高级版套餐有什么区别？",
                "我该如何取消我的订阅？",
                "我可以从年付套餐改为月付套餐吗？",
                "我想降低订阅等级，该怎么做？",
                "降低订阅等级会有罚金吗？"
        ));
        examples.put(TECHNICAL_SUPPORT, asList(
                "这个应用每次打开就崩溃。",
                "我在设置里保存不了更改。",
                "为什么搜索功能用不了了？",
                "安装程序卡在 50% 不动了。",
                "我一直收到“安装失败”的提示。",
                "我该如何在 Mac 上安装这个？",
                "我无法连接到服务器。",
                "为什么我老是掉线？",
                "我的 Wi-Fi 是好的，但你们的应用提示没有网络连接。",
                "为什么这个应用这么慢？",
                "我在视频通话时老是卡顿。",
                "这个网站在我的浏览器上一直卡死。",
                "我遇到了“404 Not Found”错误。",
                "“Permission Denied”错误是什么意思？",
                "为什么我收到了“存储空间不足”的警告？",
                "这个兼容 Windows 11 吗？",
                "这个应用在我的安卓手机上无法运行。",
                "你们有没有适用于 Safari 的浏览器扩展？"
        ));
        examples.put(ACCOUNT_MANAGEMENT, asList(
                "我忘了密码，怎么重置？",
                "我没有收到密码重置邮件。",
                "可以在应用内修改我的密码吗？",
                "我该如何设置双重身份验证？",
                "我的手机丢了，现在该怎么登录？",
                "为什么我收不到双重验证码？",
                "我的账号被锁了，怎么办？",
                "登录尝试次数有限制吗？",
                "我无缘无故被锁定登不进去，你能帮帮我吗？",
                "我该如何更改我的邮箱地址？",
                "我可以更新我的个人资料照片吗？",
                "我该如何修改我的收货地址？",
                "我可以和家人共用我的账号吗？",
                "如何给我的团队成员管理员权限？",
                "有访客访问功能吗？",
                "我该如何删除我的账号？",
                "如果我停用账号，我的数据会怎样？",
                "我以后还可以重新激活我的账号吗？"
        ));
        examples.put(PRODUCT_INFORMATION, asList(
                "“同步”功能是干什么的？",
                "隐私模式是怎么工作的？",
                "你能解释一下实时追踪功能吗？",
                "新型号什么时候到货？",
                "这个商品有中号吗？",
                "售罄的商品会很快补货吗？",
                "1.0 和 2.0 版本有什么区别？",
                "Pro 版值得多花钱吗？",
                "旧版本支持这个新更新吗？",
                "这个产品和 iOS 兼容吗？",
                "这个能用在 220V 电源上吗？",
                "你们有 USB-C 接口的选项吗？",
                "包含任何配件吗？",
                "你们卖这个型号的保护壳吗？",
                "你会推荐哪些附加组件？",
                "保修范围包括什么？",
                "我该如何申请保修？",
                "保修是全球联保的吗？"
        ));
        examples.put(ORDER_STATUS, asList(
                "我的订单现在到哪里了？",
                "你能给我一个追踪单号吗？",
                "我怎么知道我的订单已经发货了？",
                "我可以更改配送方式吗？",
                "你们提供次日达配送吗？",
                "可以选择到店自提吗？",
                "我的订单什么时候能到？",
                "为什么我的配送延迟了？",
                "我可以指定一个收货日期吗？",
                "已经过了收货日期，我的订单在哪儿？",
                "如果出现延迟，会通知我吗？",
                "天气会耽误我的包裹多久？",
                "我收到订单了，但少了一件商品。",
                "包裹到货时是空的。",
                "我收到了错误的商品，该怎么办？",
                "我买的所有东西会一起到吗？",
                "为什么我只收到了部分订单？",
                "剩下的商品能尽快发给我吗？"
        ));
        examples.put(RETURNS_AND_EXCHANGES, asList(
                "你们的退货政策是怎样的？",
                "退货的运费是免的吗？",
                "退货需要保留原包装吗？",
                "我该如何获取退货标签？",
                "退货需要先联系客服吗？",
                "需要 RMA 退货授权码吗？",
                "我需要换个不同的尺码。",
                "收到的礼物可以换吗？",
                "换货流程要多久？",
                "我的商品到货时就损坏了，怎么办？",
                "这个产品和我收到时的描述不符。",
                "少了一个零件，你们能补发吗？",
                "我收到了错误的商品，怎么更正？",
                "我没有下这个单，为什么给我寄来了？",
                "你们误给我寄了两个同样的商品。",
                "退货要收取上架费吗？",
                "我会得到全额退款吗？",
                "重新上架会扣多少钱？"
        ));
        examples.put(FEEDBACK_AND_COMPLAINTS, asList(
                "材质质量和广告中说的不符。",
                "这个产品用了一周就坏了。",
                "第一次洗涤后颜色就褪了。",
                "客服代表对我态度很差。",
                "我等待了 30 分钟，这让人无法接受。",
                "你们的客服很快解决了我的问题，谢谢！",
                "你们的网站很难浏览。",
                "这个应用老是崩溃，太让人恼火了。",
                "结账流程让人困惑。",
                "你们应该提供聊天功能以便更快地获得帮助。",
                "你们可以增加一个心愿单功能吗？",
                "请做一个适配手机的网站版本。",
                "我在你们的软件里发现了一个 bug。",
                "你们首页上有个错别字。",
                "支付页面有点问题。",
                "你们能提供无麸质的选项吗？",
                "请增加对 Linux 的支持。",
                "我希望你们能提供更多颜色可选。"
        ));

        // 创建本地 Embedding 模型（在 Java 进程内离线运行，无需联网）
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        // 用 Embedding 模型 + 各分类示例文本构建一个文本分类器
        TextClassifier<CustomerServiceCategory> classifier = new EmbeddingModelTextClassifier<>(embeddingModel, examples);

        // 对一条待分类文本进行分类：模型会返回最可能的类别（可多个）
        List<CustomerServiceCategory> categories = classifier.classify("喂，我的包裹在哪儿？");

        System.out.println(categories); // [ORDER_STATUS]（订单状态）
    }
}
