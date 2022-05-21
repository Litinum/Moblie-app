using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using Microsoft.Net.Http.Headers;
using System;
using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.IO;
using System.Linq;
using System.Security.Claims;
using System.Text;
using System.Threading.Tasks;
using WebAPI.Models;

namespace WebAPI.Helpers
{
    public class JwtHelper : IJwtHelper
    {
        private readonly IConfiguration config;
        private static List<(uint, string)> activeTokens = new List<(uint, string)>();
        public JwtHelper(IConfiguration configuration)
        {
            config = configuration;
        }

        public BinaryReader JwtRegisterClaimNames { get; private set; }
        public object SymetricSecurityKey { get; private set; }

        public static bool TokenUnverified(uint userId, HttpRequest request)
        {
            string expectedToken = CheckActiveToken(userId);
            string receivedToken = request.Headers[HeaderNames.Authorization].ToString().Replace("Bearer ", "");

            if (expectedToken == null || !expectedToken.Equals(receivedToken))
                return true;

            return false;
        }

        public string CreateToken(User model)
        {
            Claim[] claims = new[]
            {
                new Claim(ClaimTypes.Name, model.Username),
                //new Claim(JwtRegisteredClaimNames.Sub, model.Username),
                new Claim(ClaimTypes.NameIdentifier, model.Id.ToString())
            };

            SymmetricSecurityKey key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(config["Jwt:Secret"]));
            SigningCredentials creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            JwtSecurityToken jst = new JwtSecurityToken(
                issuer: config["Jwt:Issuer"],
                audience: config["Jwt:Audience"],
                claims,
                expires: DateTime.Now.AddMinutes(180),
                signingCredentials: creds
                );

            string token = new JwtSecurityTokenHandler().WriteToken(jst);

            return token;
        }

        public static string CheckActiveToken(uint userId)
        {
            foreach ((uint uId, string token) in activeTokens)
            {
                if (uId == userId)
                {
                    return token;
                }
            }

            return null;
        }

        public static bool CheckActiveToken(string token)
        {
            foreach ((uint, string) t in activeTokens)
            {
                if (t.Item2 == token)
                {
                    return true;
                }
            }

            return false;
        }

        public static void AddActiveToken(uint userId, string token)
        {
            string temp = CheckActiveToken(userId);

            if (temp != null)
                RemoveToken(userId);

            activeTokens.Add((userId, token));
        }

        public static void RemoveToken(uint userId)
        {
            foreach ((uint uId, string token) t in activeTokens)
            {
                if (t.Item1 == userId)
                {
                    activeTokens.Remove(t);
                    return;
                }
            }
        }
    }
}
