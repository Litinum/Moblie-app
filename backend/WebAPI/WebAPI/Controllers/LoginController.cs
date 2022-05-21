using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using WebAPI.Helpers;
using WebAPI.Models;
using WebAPI.Services;

// For more information on enabling Web API for empty projects, visit https://go.microsoft.com/fwlink/?LinkID=397860

namespace WebAPI.Controllers
{
    [AllowAnonymous]
    [Route("api/[controller]")]
    [ApiController]
    public class LoginController : ControllerBase
    {

        private readonly SkuciSeDBContext ctx;
        private readonly SkuciSeEmailService ems;
        private readonly IJwtHelper jwtHelper;
        private readonly IHttpContextAccessor httpContextAccessor;
        
        public LoginController(SkuciSeDBContext _ctx, SkuciSeEmailService _ems, IJwtHelper _jwtHelper, IHttpContextAccessor _httpContextAccessor)
        {
            ctx = _ctx;
            ems = _ems;
            jwtHelper = _jwtHelper;
            httpContextAccessor = _httpContextAccessor;
        }

        [HttpGet]       // DEBUG
        [Route("get_all_users")]
        public ActionResult<DbSet<User>> GetAllUsers()
        {
            return ctx.Users;
        }       ///////////////

        [HttpPost]
        [Route("user_login")]
        public ActionResult<object> UserLogin(string usernameOrEmail, string password)
        {
            if (String.IsNullOrWhiteSpace(usernameOrEmail) || String.IsNullOrWhiteSpace(password))
                return BadRequest("Korisničko ime/email ili lozinka nisu uneti.");

            var exists = ctx.Users.Where(u => (u.Username == usernameOrEmail || u.Email == usernameOrEmail) && u.Password == password);

            if (exists.Any())
            {
                User user = exists.FirstOrDefault();

                if (user.Confirmed == false)
                    return BadRequest("Vaš email nije potvrđen.");

                string token = jwtHelper.CreateToken(user);
                JwtHelper.AddActiveToken(user.Id, token);

                return Ok(new { Token = token, UserId =  user.Id, Username = user.Username});
            }

            return NotFound("Uneli ste pogrešno korisničko ime/email ili lozinku.");

        }

        [HttpPost]
        [Route("user_register")]
        public ActionResult<string> RegisterUser(string firstName, string lastName, string email, string username, string password)
        {
            Regex imePrezimeReg = new Regex(@"^([ \u00c0-\u01ffa-zA-Z'\-])+$");
            Regex usernameReg = new Regex(@"^[A-Za-z0-9_-]{4,16}$");
            Regex emailReg = new Regex(@"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$");
            Regex passReg = new Regex(@"^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,32}$");

            if (!(imePrezimeReg.IsMatch(firstName) && imePrezimeReg.IsMatch(lastName) && emailReg.IsMatch(email)
                    && usernameReg.IsMatch(username) && passReg.IsMatch(password)))
                return BadRequest("Neki od unetih podataka su neispravni.");

            User newUser = new User { Username = username, Password = password, FirstName = firstName, LastName = lastName, Email = email, DateCreated = DateTime.Now, Confirmed = false };

            try
            {
                ems.SendConfirmationEmail(newUser.Email);
                ctx.Users.Add(newUser);
                ctx.SaveChanges();

                return Ok("Korisnik je uspešno dodat.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri dodavanju korisnika.");
            }
        }

        [HttpPost]
        [Route("send_confirmation_email")]
        public ActionResult<string> SendConfirmation(string username)
        {
            User user = ctx.Users.Where(u => u.Username == username && !u.Confirmed).FirstOrDefault();

            if(user == null)
            {
                return BadRequest("Korisnik ne postoji.");
            }

            try
            {
                ems.SendConfirmationEmail(user.Email);
                return Ok("Mail poslat.");
            }
            catch
            {
                return StatusCode(500, "Greška pri slanju maila.");
            }
        }

        [HttpPost]
        [Route("send_pass_reset_email")]
        public ActionResult<string> SendPasswordReset(string usernameOrEmail)
        {
            User user = ctx.Users.Where(u => u.Username == usernameOrEmail || u.Email == usernameOrEmail).FirstOrDefault();

            if (user == null)
            {
                return BadRequest("Korisnik nije pronađen.");
            }

            try
            {
                ems.SendPasswordResetEmail(user.Email);
                return Ok("Mail za resetovanje lozinke je uspešno poslat.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri slanju maila.");
            }
        }

        [Authorize]
        [HttpPost]
        [Route("user_logout")]
        public ActionResult<string> UserLogout()
        {
            string temp = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value;

            if (temp == null)
                return NotFound("Token nije validan.");

            uint userId = uint.Parse(temp);

            JwtHelper.RemoveToken(userId);

            return Ok("Uspešno ste se izlogovali.");
        }

        [AllowAnonymous]
        [HttpPost]
        [Route("check_token")]
        public ActionResult<string> CheckToken(string token)
        {
            if (JwtHelper.CheckActiveToken(token))
                return Ok("Token postoji.");

            return NotFound("Token ne postoji.");
        }
    }
}
