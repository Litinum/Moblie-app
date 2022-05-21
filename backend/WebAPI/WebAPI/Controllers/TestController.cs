using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using WebAPI.Models;
using WebAPI.Services;

namespace WebAPI.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class TestController : ControllerBase
    {
        SkuciSeDBContext ctx;
        SkuciSeEmailService ems;

        public TestController(SkuciSeDBContext _ctx, SkuciSeEmailService _ems)
        {
            ctx = _ctx;
            ems = _ems;
        }

        [HttpGet("get_user")]
        public ActionResult<User> GetUser([FromForm] uint id)
        {
            return ctx.Users.Find(id);
        }

        [HttpGet("get_all_users")]
        public ActionResult<DbSet<User>> GetUsers()
        {
            return ctx.Users;
        }

        [HttpGet("get_all_adverts")]
        public ActionResult<DbSet<Advert>> GetAdverts()
        {
            return ctx.Adverts;
        }

        [HttpGet("get_all_unconfirmed")]
        public ActionResult<IEnumerable<User>> GetUnconfirmed()
        {
            return ctx.Users.Where(u => !u.Confirmed).ToList();
        }

        [HttpGet("get_all_confirmed")]
        public ActionResult<IEnumerable<User>> GetConfirmed()
        {
            return ctx.Users.Where(u => u.Confirmed).ToList();
        }

        [HttpPost("add_user")]
        public ActionResult<User> AddUser([FromForm] string firstName, [FromForm] string lastName)
        {
            User newUser = new User { FirstName = firstName, LastName = lastName };
            ctx.Users.Add(newUser);
            ctx.SaveChanges();
            return newUser;
        }

        [HttpPost("request_with_object")]
        public ActionResult<object> AddUser([FromBody] object obj)
        {
            return obj;
        }

        [HttpGet("send_email")]
        public ActionResult<string> SendConfirmEmail(string email)
        {
            try
            {
                ems.SendConfirmationEmail(email);
                //ems.SendPasswordResetEmail(user);
                return "Email sent.";
            }
            catch(Exception e)
            {
                return e.Message;
            }
        }

        [HttpGet]
        [Route("testing")]
        public ActionResult<List<object>> Tester(int adsPerPage, int pageNum, string orderBy, bool ascending, string city, decimal priceMin, decimal priceMax, string saleType, decimal sizeMin, decimal sizeMax, string structureType, int numOfBedrooms, int numOfBathrooms, bool furnished, string residenceType)
        {
            //var adverts = ctx.Adverts.Where(a => (a.Price <= priceMax && a.Price >= priceMin) &&
            //                        a.SaleType.ToString() == saleType &&
            //                        (a.Size <= sizeMax && a.Size >= sizeMin) &&
            //                        a.StructureType.ToString() == structureType &&
            //                        a.NumBedrooms == numOfBedrooms &&
            //                        a.NumBathrooms == numOfBathrooms &&
            //                        a.Furnished == Furnished &&
            //                        a.ResidenceType.ToString() == ResidenceType).OrderBy(a => 3);

            List<object> returns = new List<object>();

            if (adsPerPage == 0)
                returns.Add(new { adsPerPage = 1 });
            else
                returns.Add(new { adsPerPage = adsPerPage });

            if (pageNum == 0)
                returns.Add(new { pageNum = 1 });
            else
                returns.Add(new { pageNum = pageNum });

            if (numOfBedrooms != 0)
                returns.Add(new { numOfBedrooms = 2 });

            if (numOfBathrooms != 0)
                returns.Add(new { numOfBathrooms = 1 });

            if (!string.IsNullOrWhiteSpace(orderBy))
                returns.Add(new { orderBy = orderBy });

            if(ascending)
                returns.Add(new { ascending = ascending });

            if (furnished)
                returns.Add(new { furnished = furnished });

            if (!string.IsNullOrWhiteSpace(city))
                returns.Add(new { city = city });

            if (priceMin != 0)
                returns.Add(new { priceMin = priceMin });

            if (priceMax == 0)
                returns.Add(new { priceMax = decimal.MaxValue });
            else
                returns.Add(new { priceMax = priceMax });

            if (sizeMin != 0)
                returns.Add(new { sizeMin = sizeMin });

            if (sizeMax == 0)
                returns.Add(new { sizeMax = decimal.MaxValue });
            else
                returns.Add(new { sizeMax = sizeMax });

            if (!string.IsNullOrWhiteSpace(saleType))
                returns.Add(new { saleType = saleType });

            if (!string.IsNullOrWhiteSpace(structureType))
                returns.Add(new { structureType = structureType });

            if (!string.IsNullOrWhiteSpace(residenceType))
                returns.Add(new { residenceType = residenceType });

            return returns;




        }
    
        [HttpGet]
        [Route("get_all_messages")]
        public ActionResult<IEnumerable<Message>> GetAllMessages()
        {
            return ctx.Messages.ToList();
        }

        [HttpGet]
        [Route("get_all_fav_adverts")]
        public ActionResult<IEnumerable<FavouriteAdvert>> GetAllFavAdverts()
        {
            return ctx.FavouriteAdverts.ToList();
        }

        [HttpGet]
        [Route("get_all_meetings")]
        public ActionResult<IEnumerable<Meeting>> GetAllMeetings()
        {
            return ctx.Meetings.ToList();
        }

        [HttpGet]
        [Route("get_all_reviews")]
        public ActionResult<IEnumerable<Review>> GetAllReviews()
        {
            return ctx.Reviews.ToList();
        }

        [HttpPost]
        [Route("delete_user")]
        public ActionResult<string> DeleteUser(string username)
        {
            var result = ctx.Users.Where(u => u.Username == username).FirstOrDefault();

            if (result == null)
                return NotFound("User does not exist.");

            try
            {
                ctx.Users.Remove(result);
                ctx.SaveChanges();
                return Ok("User deleted.");
            }
            catch
            {
                return StatusCode(500, "Failed to delete user");
            }
        }
    }
}