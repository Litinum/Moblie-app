using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Models;
using WebAPI.Services;

namespace WebAPI.Controllers
{
    [AllowAnonymous]
    [Route("link")]
    public class LinkController : Controller
    {
        SkuciSeDBContext ctx;
        SkuciSeEmailService ems;

        public LinkController(SkuciSeDBContext _ctx, SkuciSeEmailService _ems)
        {
            ctx = _ctx;
            ems = _ems;
        }
        
        public IActionResult Index()
        {
            return View("Error");
            //return View("PasswordReset");
        }

        [HttpGet]
        [Route("confirm_email")]
        public IActionResult ConfirmEmail(string token)
        {
            string userEmail = ems.FindConfirmationRequest(token);

            User user = ctx.Users.Where(user => user.Email == userEmail && !user.Confirmed).FirstOrDefault();
            if (user != null)
            {
                user.Confirmed = true;
                ctx.Users.Update(user);
                ctx.SaveChanges();

                return View("ConfirmEmail");
            }
            else
            {
                return View("Error");
            }
        }

        [HttpGet]
        [Route("reset_password")]
        public IActionResult ResetPassword(string token)
        {
            if (token != null)
            {
                return View(viewName: "PasswordReset", model: token);
            }
            else
            {
                return View("Error");
            }
        }

        [HttpPost]
        [Route("new_password")]
        public IActionResult NewPassword(string token, string password, string passwordR)
        {
            string userEmail = ems.FindResetRequest(token);

            User user = ctx.Users.Where(user => user.Email == userEmail).FirstOrDefault();

            if (user != null && token != null && password == passwordR)
            {
                //To Do: Perform regex on password
                user.Password = password;
                ctx.Users.Update(user);
                ctx.SaveChanges();
                return View("PasswordResetSuccess");
            }
            else
            {
                return View("Error");
            }
        }
    }
}
