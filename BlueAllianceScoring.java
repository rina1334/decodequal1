package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.hardware.motors.Motor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Autonomous(name = "Limelight + OTOS + Pedro Auto", group = "Test")
public class BlueAllianceScoring extends LinearOpMode {
//prewrite pathing and sorting (servo)
    //play around with pipelines to get color sensor? hsv color masking
    private static final int[] VALID_IDS = {21, 22, 23};
    private Motor frontLeft, frontRight, backLeft, backRight;

    @Override
    public void runOpMode() {
        ElapsedTime runtime = new ElapsedTime();
        telemetry.addLine("Initializing...");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive() && runtime.seconds() < 30.0) {
            telemetry.addData("Time", "%.1f", runtime.seconds());
            telemetry.update();

            int detectedTag = getAprilTagFromLimelight();

            if (detectedTag != -1) {
                telemetry.addData("Detected Tag", detectedTag);

                switch (detectedTag) {
                    case 21:
                        telemetry.addLine("Performing Action A (Path A)");
                        // Example: pathFollower.followPath(pathA);
                        break;
                    case 22:
                        telemetry.addLine("Performing Action B (Path B)");
                        break;
                    case 23:
                        telemetry.addLine("Performing Action C (Path C)");
                        break;
                }
            } else {
                telemetry.addLine("No valid AprilTag detected");
            }

            telemetry.update();
            sleep(200); // prevent spamming
        }
        stopMotors();
    }

    private void stopMotors() {
        //stop motors here
        frontLeft.set(0);
        frontRight.set(0);
        backLeft.set(0);
        backRight.set(0);
    }

    private int getAprilTagFromLimelight() {
        try {
            // Replace "limelight.local" with your Limelight's IP if needed
            URL url = new URL("http://limelight.local/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(50);
            conn.setReadTimeout(50);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            String json = content.toString();

            // Very rough parse to find "tid" (tag ID)
            if (json.contains("\"tid\":")) {
                String[] split = json.split("\"tid\":");
                if (split.length > 1) {
                    String idString = split[1].split(",")[0].trim();
                    int id = Integer.parseInt(idString);
                    for (int valid : VALID_IDS) {
                        if (id == valid) {
                            return id;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Network might not always be available
        }
        return -1;
    }
}