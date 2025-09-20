package com.back_hexiang_studio.enumeration;

/**
 * AI随机消息枚举
 */
public enum AiRandomMessage {
    WORK_SMOOTH("🌟 今天工作顺利吗？记得适时休息哦！", "work"),
    PROGRESS("💪 你今天又进步了一点，加油！", "encouragement"),
    HELP_QUERY("🎯 有什么需要我帮助查询的吗？", "help"),
    TAKE_BREAK("☕ 工作忙碌，别忘记喝水休息～", "health"),
    DATA_STATS("📊 想了解最新的数据统计吗？", "data"),
    SYSTEM_QUERY("🔍 我可以帮你查询系统中的任何信息", "system"),
    MOOD_CHECK("✨ 今天的心情怎么样？有什么想聊的？", "mood"),
    EFFICIENCY("🚀 一起来提高工作效率吧！", "efficiency"),
    LEARNING("📚 最近有新的学习计划吗？", "learning"),
    INSPIRATION("🎨 创意工作需要灵感，我来帮你找找～", "creativity"),
    ACHIEVEMENT("⭐ 今天有什么收获吗？", "achievement"),
    POSITIVE("🌈 保持好心情，工作更有效率！", "positive"),
    GREETING_MORNING("🌅 早上好！新的一天开始啦，准备好迎接挑战了吗？", "greeting"),
    GREETING_AFTERNOON("🌞 下午好！工作进展如何？需要我帮你查点什么吗？", "greeting"),
    GREETING_EVENING("🌙 晚上好！辛苦工作一天了，有什么想了解的吗？", "greeting");

    private final String message;
    private final String category;

    AiRandomMessage(String message, String category) {
        this.message = message;
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }
}