using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Net.Http.Headers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;
using WebAPI.Helpers;
using WebAPI.Models;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    [ApiController]
    public class ReviewController : Controller
    {
        SkuciSeDBContext ctx;

        private readonly string username;
        private readonly uint userId;

        public ReviewController(SkuciSeDBContext _ctx, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            username = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.Name)?.Value;
            uint.TryParse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out userId);
        }

        [HttpPost]
        [Route("post_review")]
        public ActionResult<string> PostReview(uint advertId, uint rating, string text)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            var result = ctx.Meetings.Where(m => m.AdvertId == advertId && m.VisitorId == userId && m.Concluded == true && !ctx.Reviews.Any(r => r.MeetingId == m.Id)).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, ne možete postaviti recenziju sa ovaj oglas jer sastanak ne postoji ili nije završen.");

            Review newReview = new Review { MeetingId = result.Id, Rating = rating, Text = text };

            try
            {
                ctx.Reviews.Add(newReview);
                ctx.SaveChanges();
                return Ok("Recenzija uspešno postavljena.");
            }
            catch
            {
                return StatusCode(500, "Greška pri postavljanju recenzije.");
            }
        }

        [HttpPost]
        [Route("get_my_available_reviews")]
        public ActionResult<IEnumerable<object>> AvailableReviews()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            var result = ctx.Meetings.Where(m => m.VisitorId == userId && m.Concluded == true && !ctx.Reviews.Any(r => r.MeetingId == m.Id)).ToList();

            return result;
        }

        [HttpPost]
        [Route("calculate_advert_rating")]
        public ActionResult<string> CalculateAdvertRating(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return AverageAdvertRating(ctx, advertId);
        }

        [HttpPost]
        [Route("calculate_user_rating")]
        public ActionResult<string> CalculateUserRating(uint idUser)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return AverageUserRating(ctx, idUser);
        }
        
        [HttpPost]
        [Route("get_advert_reviews")]
        public ActionResult<IEnumerable<object>> GetAdvertReviews(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return ctx.Meetings.Where(m => m.AdvertId == advertId)
                    .Join(ctx.Reviews, m => m.Id, r => r.MeetingId, (m, r) => new { m.VisitorId, r })
                    .Join(ctx.Users, j => j.VisitorId, u => u.Id, (j, u) => new { UserId = u.Id, Username = u.FirstName + " " + u.LastName, Rating = j.r.Rating, Text = j.r.Text }).ToList();
        }
        public static string AverageAdvertRating(SkuciSeDBContext ctx, uint advertId)
        {
            var result = ctx.Meetings.Where(m => m.AdvertId == advertId)
                            .Join(ctx.Reviews, m => m.Id, r => r.MeetingId, (m, r) => r);

            if (result.Any())
                return result.Average(r => r.Rating).ToString();
            return "Nije ocenjeno.";
        }

        public static string AverageUserRating(SkuciSeDBContext ctx, uint idUser)
        {
            var result = ctx.Adverts.Where(ad => ad.OwnerId == idUser)
                            .Join(ctx.Meetings, ad => ad.Id, m => m.AdvertId, (ad, m) => new { ad.Id, m })
                            .Join(ctx.Reviews, j => j.m.Id, r => r.MeetingId, (j, r) => new { j.Id, r })
                            .GroupBy(j => j.Id)
                            .Select(j => new { Advert = j.Key, AverageRating = j.Average(x => x.r.Rating) });

            if (result.Any())
                return result.Average(x => x.AverageRating).ToString();
            return "Korisnik nije ocenjen.";
        }

        public static bool CanLeaveReview(SkuciSeDBContext ctx, uint advertId, uint userId)
        {
            var result = ctx.Meetings.Where(m => m.VisitorId == userId && m.AdvertId == advertId 
                        && m.Concluded == true && !ctx.Reviews.Any(r => r.MeetingId == m.Id)).FirstOrDefault();

            if (result != null)
                return true;

            return false;
        }
    }
}
