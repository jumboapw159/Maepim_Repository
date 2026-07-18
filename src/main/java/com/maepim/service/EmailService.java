package com.maepim.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("MAEPIM - Password Reset Verification Code");
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">

                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                        <td align="center">

                            <table width="600" cellpadding="0" cellspacing="0"
                                   style="background:#ffffff;border-radius:10px;overflow:hidden;">

                                <!-- Header -->
                                <tr>
                                    <td align="center"
                                        style="background:#ee7bf9;padding:30px;color:white;">

                                        <h1 style="margin:0;">MAEPIM</h1>

                                        <p style="margin-top:10px;">
                                            Sportswear Ordering Platform
                                        </p>

                                    </td>
                                </tr>

                                <!-- Body -->
                                <tr>
                                    <td style="padding:40px;">

                                        <h2>Password Reset Request</h2>

                                        <p style="font-size:16px;color:#555;">
                                            We received a request to reset your password.
                                        </p>

                                        <p style="font-size:16px;color:#555;">
                                            Please use the following One-Time Password (OTP)
                                            to continue.
                                        </p>

                                        <div style="
                                            text-align:center;
                                            margin:35px 0;">

                                            <span style="
                                                display:inline-block;
                                                background:#ee7bf9;
                                                color:white;
                                                padding:18px 40px;
                                                border-radius:8px;
                                                font-size:34px;
                                                font-weight:bold;
                                                letter-spacing:10px;">

                                                %s

                                            </span>

                                        </div>

                                        <p style="font-size:15px;color:#666;">
                                            This OTP will expire in
                                            <strong>5 minutes</strong>.
                                        </p>

                                        <p style="font-size:15px;color:#666;">
                                            Do not share this code with anyone.
                                        </p>

                                        <hr style="margin:30px 0;">

                                        <p style="font-size:13px;color:#999;">
                                            If you did not request a password reset,
                                            you can safely ignore this email.
                                        </p>

                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td align="center"
                                        style="background:#f7f7f7;padding:20px;color:#888;font-size:13px;">

                                        © 2026 MAEPIM

                                        <br>

                                        This is an automated email.
                                        Please do not reply.

                                    </td>
                                </tr>

                            </table>

                        </td>
                    </tr>
                </table>

                </body>
                </html>
                """.formatted(otp);
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("OTP email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send OTP email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email.", e);
        }
    }
}