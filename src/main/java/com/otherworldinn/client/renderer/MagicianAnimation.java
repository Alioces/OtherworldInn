package com.otherworldinn.client.renderer;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class MagicianAnimation {
    public static final AnimationDefinition LOOP =
            AnimationDefinition.Builder.withLength(2.4F)
                    .looping()
                    .addAnimation(
                            "head",
                            new AnimationChannel(
                                    AnimationChannel.Targets.ROTATION,
                                    new Keyframe(
                                            0.0F,
                                            KeyframeAnimations.degreeVec(0.0F, -3.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            1.2F,
                                            KeyframeAnimations.degreeVec(2.0F, 3.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            2.4F,
                                            KeyframeAnimations.degreeVec(0.0F, -3.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM)))
                    .addAnimation(
                            "body",
                            new AnimationChannel(
                                    AnimationChannel.Targets.ROTATION,
                                    new Keyframe(
                                            0.0F,
                                            KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            1.2F,
                                            KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            2.4F,
                                            KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
                                            AnimationChannel.Interpolations.CATMULLROM)))
                    .addAnimation(
                            "right_arm",
                            new AnimationChannel(
                                    AnimationChannel.Targets.ROTATION,
                                    new Keyframe(
                                            0.0F,
                                            KeyframeAnimations.degreeVec(-8.0F, -12.0F, 2.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            1.2F,
                                            KeyframeAnimations.degreeVec(-28.0F, -24.0F, 8.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            2.4F,
                                            KeyframeAnimations.degreeVec(-8.0F, -12.0F, 2.0F),
                                            AnimationChannel.Interpolations.CATMULLROM)))
                    .addAnimation(
                            "left_arm",
                            new AnimationChannel(
                                    AnimationChannel.Targets.ROTATION,
                                    new Keyframe(
                                            0.0F,
                                            KeyframeAnimations.degreeVec(-12.0F, 10.0F, -2.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            1.2F,
                                            KeyframeAnimations.degreeVec(-34.0F, 20.0F, -8.0F),
                                            AnimationChannel.Interpolations.CATMULLROM),
                                    new Keyframe(
                                            2.4F,
                                            KeyframeAnimations.degreeVec(-12.0F, 10.0F, -2.0F),
                                            AnimationChannel.Interpolations.CATMULLROM)))
                    .build();

    private MagicianAnimation() {}
}
