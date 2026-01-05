package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.hardware.motors.Motor;

@TeleOp(name = "ContinuousServoFixed")
public class ContinuousServoFixed extends OpMode {

    private GamepadEx gp2;
    private Servo pushServo;
    static final double ONE_TURN = 1.0 / 4.16;
    static final double POS_0   = 0.0;
    static final double POS_120 = 0.444;
    static final double POS_240 = 0.888;

    @Override
    public void init() {
        pushServo = hardwareMap.get(Servo.class, "push_servo");
        gp2 = new GamepadEx(gamepad2);
        pushServo.setPosition(POS_0);
    }

    @Override
    public void loop() {
        if(gamepad2.x){
            pushServo.setPosition(POS_0);
        }
        if (gamepad2.y) {
            pushServo.setPosition(POS_120);
        }
        if (gamepad2.b) {
            pushServo.setPosition(POS_240);
        }
    }
}
