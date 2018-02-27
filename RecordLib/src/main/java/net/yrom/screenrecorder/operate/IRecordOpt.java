package net.yrom.screenrecorder.operate;

/**
 * Created by daven.liu on 2018/2/27 0027.
 */

public interface IRecordOpt {
    /**
     * 结束录制（直播）
     */
    void stopRecord();

    /**
     * 是否正在录制（直播）
     * @return
     */
    boolean isRecording();

    /**
     * 暂停录制（直播）
     */
    void pause();

    /**
     * 恢复录制（直播）
     */
    void resume();

    /**
     * 是否启用麦克风
     * @return
     */
    boolean isMic();

    /**
     * 设置是否启用麦克风
     * @param mute
     */
    void setMic(boolean mute);

}
