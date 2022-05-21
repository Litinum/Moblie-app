using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Net.Http.Headers;
using WebAPI.Helpers;
using WebAPI.Models;
using WebAPI.Services;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    [ApiController]
    public class UsersController : ControllerBase
    {
        SkuciSeDBContext ctx;
        SkuciSeEmailService ems;

        private readonly string username;
        private readonly uint userId;

        public UsersController(SkuciSeDBContext _ctx, SkuciSeEmailService _ems, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            ems = _ems;
            username = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.Name)?.Value;
            uint.TryParse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out userId);
        }

        [HttpPost]
        [Route("get_my_info")]
        public ActionResult<object> GetMyInfo()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return ctx.Users.Where(u => u.Id == userId).Select(u => new { u.Id, u.Username, u.FirstName, u.LastName, u.Email, u.DateCreated, NumberOfAdverts = UsersController.NumOfAdverts(ctx, userId), UserScore = ReviewController.AverageUserRating(ctx, userId) }).FirstOrDefault();
        }

        [HttpPost]
        [Route("get_user_info")]
        public ActionResult<object> GetUserInfo(uint idUser)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return ctx.Users.Where(u => u.Id == idUser).Select(u => new { u.Id, u.Username, u.FirstName, u.LastName, NumberOfAdverts = UsersController.NumOfAdverts(ctx, idUser), UserScore = ReviewController.AverageUserRating(ctx, idUser) }).FirstOrDefault();
        }

        [HttpPost]
        [Route("send_pass_reset_email")]
        public ActionResult ResetPassword()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            User user = ctx.Users.Where(u => u.Username == username).FirstOrDefault();
            try
            {
                ems.SendPasswordResetEmail(user.Email);
                return Ok("Zahtev za promenu lozinke uspešno poslat.");
            }
            catch
            {
                return StatusCode(500, "Greška pri slanju zahteva za promenu lozinke.");
            }
        }

        [HttpPost]
        [Route("change_email")]
        public ActionResult ChangeEmail(string newEmail)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Regex emailReg = new Regex(@"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$");

            if (!emailReg.IsMatch(newEmail))
                return BadRequest("Greška, podatak je neispravan.");

            User result = ctx.Users.Where(u => u.Id == userId).FirstOrDefault();

            if(result != null)
            {
                try
                {
                    result.Email = newEmail;
                    result.Confirmed = false;           // email is not confirmed anymore.
                    ctx.Users.Update(result);
                    ctx.SaveChanges();
                    ems.SendConfirmationEmail(newEmail);
                    return Ok("Email adresa uspešno promenjena. Potvrdite novu email adresu.");
                }
                catch
                {
                    return StatusCode(500, "Greška pri izmeni email adrese.");
                }
            }

            return NotFound("Greška, korisnik ne postoji.");
        }

        [HttpPost]
        [Route("change_password")]
        public ActionResult ChangePassword(string oldPassword, string newPassword)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            User result = ctx.Users.Where(u => u.Id == userId && u.Password == oldPassword).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, Stara lozinka nije tačna.");

            Regex passReg = new Regex(@"^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,32}$");

            if (!passReg.IsMatch(newPassword))
                return BadRequest("Greška, podatak je neispravan.");

            try
            {
                result.Password = newPassword;
                ctx.Users.Update(result);
                ctx.SaveChanges();
                return Ok("Lozinka uspešno promenjena.");
            }
            catch
            {
                return StatusCode(500, "Greška pri izmeni lozinke.");
            }
        }

        [HttpPost]
        [Route("change_user_info")]
        public ActionResult ChangeInfo(string newUsername, string newFirstName, string newLastName)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Regex imePrezimeReg = new Regex(@"^([ \u00c0-\u01ffa-zA-Z'\-])+$");
            Regex usernameReg = new Regex(@"^[A-Za-z0-9_-]{4,16}$");

            User result = ctx.Users.Where(u => u.Id == userId).FirstOrDefault();

            if (newUsername != null)
            {
                if (!usernameReg.IsMatch(newUsername))
                    return BadRequest("Greška, korisničko ime je neispravno.");

                result.Username = newUsername;
            }
                

            if (newFirstName != null)
            {
                if (!imePrezimeReg.IsMatch(newFirstName))
                    return BadRequest("Greška, ime je neispravno.");

                result.FirstName = newFirstName;
            }
                

            if (newLastName != null)
            {
                if (!imePrezimeReg.IsMatch(newLastName))
                    return BadRequest("Greška, prezime je neispravno.");

                result.LastName = newLastName;
            }

            ctx.Users.Update(result);
            ctx.SaveChanges();

            return Ok("Podaci uspešno izmenjeni.");
        }

        public static int NumOfAdverts(SkuciSeDBContext ctx, uint idUser)
        {
            return ctx.Adverts.Where(ad => ad.OwnerId == idUser).Count();
        }
    }
}