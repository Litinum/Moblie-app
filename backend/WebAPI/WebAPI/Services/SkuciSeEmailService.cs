using System;
using System.IO;
using Microsoft.Extensions.Configuration;
using System.Collections;
using WebAPI.Models;
using System.Collections.Generic;

namespace WebAPI.Services
{
    public class SkuciSeEmailService : EmailService
    {
        private readonly IConfiguration config;
        private readonly Random rgen;
        private string from, smtp, pass;
        private int port;
        private string domain;

        private string confirmTemplate, passwordResetTemplate;

        private Dictionary<string, TimedRequest> confirmationRequests;
        private Dictionary<string, TimedRequest> resetRequests;

        public SkuciSeEmailService(IConfiguration _config)
        {
            config = _config;
            ReadConfig();
            rgen = new Random();

            confirmationRequests = new Dictionary<string, TimedRequest>();
            resetRequests = new Dictionary<string, TimedRequest>();
        }

        public string FindConfirmationRequest(string token)
        {
            TimeSpan timeLimit = new TimeSpan(hours: 0, minutes: 30, seconds: 0);
            return FindRequest(token, confirmationRequests, timeLimit);
        }

        public string FindResetRequest(string token)
        {
            TimeSpan timeLimit = new TimeSpan(hours: 0, minutes: 15, seconds: 0);
            return FindRequest(token, resetRequests, timeLimit);
        }

        public void SendConfirmationEmail(string email)
        {
            string token = GenerateToken();
            confirmationRequests[token] = new TimedRequest(email, DateTime.Now);

            string confirmationLink = $"{domain}/link/confirm_email?token={token}";

            const string title = "SkućiSe: Potvrdite vašu e-mail adresu...";
            string body = string.Format(confirmTemplate, confirmationLink, from);

            Send(from, email, title, body, smtp, port, from, pass);
        }

        public void SendPasswordResetEmail(string email)
        {
            string token = GenerateToken();
            resetRequests.Add(token, new TimedRequest(email, DateTime.Now));

            string resetLink = $"{domain}/link/reset_password?token={token}";

            const string title = "SkućiSe: Zaboravljena lozinka...";
            string body = string.Format(passwordResetTemplate, resetLink, from);

            Send(from, email, title, body, smtp, port, from, pass);
        }

        private Dictionary<int, string> IntToHex = new Dictionary<int, string>
        {
            [0] = "0",
            [1] = "1",
            [2] = "2",
            [3] = "3",
            [4] = "4",
            [5] = "5",
            [6] = "6",
            [7] = "7",
            [8] = "8",
            [9] = "9",
            [10] = "A",
            [11] = "B",
            [12] = "C",
            [13] = "D",
            [14] = "E",
            [15] = "F"
        };

        private string GenerateToken()
        {
            string token = "";
            for(int i=0; i<=16; i++)
            {
                token += IntToHex[rgen.Next()%16];
            }
            return token;
        }

        private void UpdateRequests(Dictionary<string, TimedRequest> dict, TimeSpan timeLimit)
        {
            foreach ((var token, var request) in dict)
            {
                Console.WriteLine(DateTime.Now - request.RequestTime);
                if (DateTime.Now - request.RequestTime > timeLimit)
                {
                    dict.Remove(token);
                }
            }
        }

        private string FindRequest(string token, Dictionary<string, TimedRequest> dict, TimeSpan timeLimit)
        {
            UpdateRequests(dict, timeLimit);

            if(dict.TryGetValue(token, out TimedRequest req))
            {
                dict.Remove(token);
                return req.Email;
            }
            else
            {
                return null;
            }
        }

        private string ReadFromFile(string path)
        {
            using (StreamReader stream = new StreamReader(path))
            {
                string content = stream.ReadToEnd();
                return content;
            }
        }

        private void ReadConfig()
        {
            var smtpConfig = config.GetSection("SMTP");
            from = smtpConfig.GetValue("Email", "default@email.com");
            smtp = smtpConfig.GetValue(key: "Server", defaultValue: "smtp.gmail.com");
            port = smtpConfig.GetValue<int>(key: "Port", defaultValue: 587);
            pass = smtpConfig.GetValue(key: "Password", defaultValue: "");
            domain = smtpConfig.GetValue(key: "Domain", defaultValue: @"http://localhost:5000");

            var templates = config.GetSection("Templates");
            confirmTemplate = ReadFromFile(templates.GetValue(key: "ConfirmationEmailBody", defaultValue: "err_no_path"));
            passwordResetTemplate = ReadFromFile(templates.GetValue(key: "ForgotPasswordEmailBody", defaultValue: "err_no_path"));
        }
    }

    public class TimedRequest
    {
        public string Email { get; set; }
        public DateTime RequestTime { get; set; }

        public TimedRequest(string email, DateTime requestTime)
        {
            Email = email;
            RequestTime = requestTime;
        }
    }
}
