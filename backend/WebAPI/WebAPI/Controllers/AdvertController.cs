using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;
using WebAPI.Models;
using System.Text.Json;
using System.Text.Json.Serialization;
using WebAPI.Helpers;
using Microsoft.Net.Http.Headers;
using System.Reflection;
using WebAPI.Services;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    [ApiController]
    public class AdvertController : Controller
    {
        private readonly SkuciSeDBContext ctx;
        private readonly SkuciSeImageService img;
        private readonly string username;
        private readonly uint userId;
        public static string unAuthMsg = "Zabranjen pristup, morate da se ulogujete.";

        public AdvertController(SkuciSeDBContext _ctx, SkuciSeImageService _img, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            img = _img;
            username = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.Name)?.Value;
            uint.TryParse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out userId);

            Listing.img = img;
        }

        [HttpPost]
        [Route("get_recent_adverts")]
        public ActionResult<IEnumerable<object>> GetRecentAdverts(uint numOfAdverts)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var adverts = ctx.Adverts.OrderByDescending(ad => ad.DateCreated).Take((int)numOfAdverts).Select(Listing.AdListing);

            return adverts.ToList();

        }

        [AllowAnonymous]
        [HttpGet]
        [Route("get_all_cities")]
        public ActionResult<IEnumerable<String>> GetAllCities()
        {
            return SkuciSeDBContext.CityNames;
        }

        [HttpPost]      
        [Route("get_advert")]
        public ActionResult<object> GetAdvert(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var result = ctx.Adverts.Where(ad => ad.Id == advertId)
                .Join(ctx.Users, ad => ad.OwnerId, u => u.Id, (ad, u) => new { ad, u.Username}).FirstOrDefault();        // CHANGE LATER

            if (result != null)
                return new { AdvertData = result.ad, AverageScore = ReviewController.AverageAdvertRating(ctx, advertId), CanLeaveReview = ReviewController.CanLeaveReview(ctx, advertId, userId), Username = result.Username, IsFavourite = IsFavourite(advertId), Images = Listing.GetImages(advertId) };
            else
                return NotFound("Oglas nije pronađen.");
        }

        public class Filter
        {
            public string Name { get; set; }
            public dynamic Param { get; set; }
        }
        
        [HttpPost]
        [Route("search_adverts")]
        public ActionResult<object> SearchAdverts(string filterArray, string searchParam, uint adsPerPage, uint pageNum, string orderBy, bool ascending)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            Dictionary<string, Func<Advert, dynamic, bool>> filterDict = new Dictionary<string, Func<Advert, dynamic, bool>>
            {
                ["NumBedrooms"] = ((ad, param) => ad.NumBedrooms >= int.Parse(param.ToString())),
                ["Price"] = ((ad, param) => ad.Price >= decimal.Parse(param.GetProperty("From").ToString()) && ad.Price <= decimal.Parse(param.GetProperty("To").ToString())),
                ["City"] = ((ad, param) =>
                    {
                        var array = param.GetString().Trim('[', ']').Split(", ");
                        bool result = false;
                        foreach (var x in array)
                        {
                            result = result || ad.City.Contains(x);
                        }

                        return result;
                    }),
                ["SaleType"] = ((ad, param) => ((int)ad.SaleType).ToString() == param.ToString()),
                ["Size"] = ((ad, param) => ad.Size >= decimal.Parse(param.GetProperty("From").ToString()) && ad.Size <= decimal.Parse(param.GetProperty("To").ToString())),
                ["NumBathrooms"] = ((ad, param) => ad.NumBathrooms >= int.Parse(param.ToString())),
                ["StructureType"] = ((ad, param) =>
                {
                    var array = param.GetString().Trim('[', ']').Split(", ");
                    bool result = false;
                    foreach (var x in array)
                    {
                        result = result || ((int)ad.StructureType).ToString() == x;
                    }

                    return result;
                }),
                ["ResidenceType"] = ((ad, param) => ((int)ad.ResidenceType).ToString() == param.ToString()),
                ["Furnished"] = ((ad, param) => ad.Furnished.ToString() == param.ToString())
            };

            Dictionary<string, Func<Advert, dynamic>> orderByDict = new Dictionary<string, Func<Advert, dynamic>>
            {
                ["Id"] = (ad => ad.Id),
                ["ResidenceType"] = (ad => ad.ResidenceType),
                ["SaleType"] = (ad => ad.SaleType),
                ["StructureType"] = (ad => ad.StructureType),
                ["Title"] = (ad => ad.Title.ToString()),
                ["City"] = (ad => ad.City.ToString()),
                ["Size"] = (ad => ad.Size),
                ["Price"] = (ad => ad.Price),
                ["NumBedrooms"] = (ad => ad.NumBedrooms),
                ["NumBathrooms"] = (ad => ad.NumBathrooms),
                ["Furnished"] = (ad => ad.Furnished),
                ["YearOfMake"] = (ad => ad.YearOfMake),
                ["DateCreated"] = (ad => ad.DateCreated),
            };
            
            IEnumerable<Advert> result = ctx.Adverts;

            if(filterArray != null)
            {
                var filters = JsonSerializer.Deserialize<Filter[]>(filterArray);

                foreach (var filter in filters)
                {
                    result = result.Where(ad => filterDict[filter.Name](ad, filter.Param));
                }
            }

            if(orderBy != null)
            {
                if (ascending)
                    result = result.OrderBy(ad => orderByDict[orderBy](ad));
                else
                    result = result.OrderByDescending(ad => orderByDict[orderBy](ad));
            }

            if (searchParam != null)
            {
                //result = result.Where(ad => ad.Title.ToLower().Contains(searchParam) || ad.Description.ToLower().Contains(searchParam));

                string[] searchTerms = searchParam.Split(' ').Select(s => s.ToLower()).ToArray();
                searchTerms = SearchHelper.RemoveNoise(searchTerms);

                result = result.Where(ad => searchTerms.Any(ad.Title.ToLower().Contains));
            }

            if (adsPerPage == 0)
                adsPerPage = 10;
            if (pageNum == 0)
                pageNum = 1;

            int count = result.Count();
            result = result.Skip((int)adsPerPage * ((int)pageNum - 1)).Take((int)adsPerPage);

            return new { Count = count, Result = result.Select(Listing.AdListing).ToList() };
        }

        [HttpPost]
        [Route("get_my_adverts")]
        public ActionResult<IEnumerable<object>> GetMyAdverts()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var result = ctx.Adverts.Where(ad => ad.OwnerId == userId).Select(ad => Listing.AdListing(ad));

            return result.ToList();
        }

        [HttpPost]
        [Route("get_favourite_adverts")]
        public ActionResult<IEnumerable<object>> GetFavouriteAdverts()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var result = ctx.FavouriteAdverts.Where(f => f.UserId == userId)
                .Join(ctx.Adverts,f => f.AdvertId,ad => ad.Id,(f, ad) => Listing.AdListing(ad));

            return result.ToList();
        }

        [HttpPost]
        [Route("add_favourite_advert")]
        public ActionResult<string> AddFavouriteAdvert(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            FavouriteAdvert newFavourite = new FavouriteAdvert { UserId = userId, AdvertId = advertId };

            try
            {
                ctx.FavouriteAdverts.Add(newFavourite);
                ctx.SaveChanges();

                return Ok("Oglas uspešno dodat u listu omiljenih oglasa.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri dodavanju oglasa u listu omiljenih oglasa.");
            }
        }

        [HttpPost]
        [Route("remove_favourite_advert")]
        public ActionResult<string> RemoveFavouritAdvert(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var result = ctx.FavouriteAdverts.Where(ad => ad.AdvertId == advertId && ad.UserId == userId).FirstOrDefault();

            if (result != null)
            {
                ctx.FavouriteAdverts.Remove(result);
                ctx.SaveChanges();

                return Ok("Oglas uspešno obrisan iz liste omiljenih oglasa.");
            }

            return NotFound("Oglas ne postoji u listi omiljenih oglasa.");
        }

        [HttpPost]
        [Route("add_advert")]
        public ActionResult<uint> AddAdvert(string advertJson)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            Advert newAdvert = JsonSerializer.Deserialize<Advert>(advertJson);
            newAdvert.DateCreated = DateTime.Now;
            newAdvert.OwnerId = userId;
            newAdvert.Id = default(uint);

            try
            {
                ctx.Adverts.Add(newAdvert);
                ctx.SaveChanges();

                return newAdvert.Id;
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri kreiranju oglasa.");
            }
        }

        [HttpPost]
        [Route("remove_advert")]
        public ActionResult<string> RemoveAdvert(uint advertId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            var result = ctx.Adverts.Where(ad => ad.Id == advertId).FirstOrDefault();
            
            if(result != null)
            {
                if (result.OwnerId != userId)
                    return BadRequest("Greška, niste vlasnik oglasa.");

                ctx.Adverts.Remove(result);
                ctx.SaveChanges();

                return Ok("Oglas uspešno obrisan.");
            }

            return NotFound("Oglas ne postoji.");
        }

        //[HttpPost]
        //[Route("edit_advert")]
        //public ActionResult<string> EditAdvert(uint advertId, string editJson)
        //{
        //    dynamic editAdvert = JsonSerializer.Deserialize<dynamic>(editJson);
        //    Advert result = ctx.Adverts.Where(ad => ad.Id == advertId).FirstOrDefault();
        //    PropertyInfo[] properties = typeof(Advert).GetProperties();
            
        //    if (result == null)
        //        return NotFound("Advert does not exist.");
            
        //    foreach(PropertyInfo property in properties)
        //    {
        //        if (property.Name == "Id")
        //            continue;
        //        try
        //        {
        //            editAdvert.GetProperty(property.Name);      // ne prolazi kroz if-ove ukoliko ne postoji property

        //            if (property.PropertyType == typeof(string))
        //            {
        //                property.SetValue(result, editAdvert.GetProperty(property.Name).GetString());
        //            }
        //            else if(property.PropertyType == typeof(decimal))
        //            {
        //                property.SetValue(result, editAdvert.GetProperty(property.Name).GetDecimal());
        //            }
        //            else if(property.PropertyType == typeof(bool))
        //            {
        //                property.SetValue(result, editAdvert.GetProperty(property.Name).GetBoolean());
        //            }
        //            else if(property.PropertyType == typeof(uint))
        //            {
        //                property.SetValue(result, editAdvert.GetProperty(property.Name).GetUInt32());
        //            }
        //        }
        //        catch (Exception e)
        //        {
        //            continue;
        //        }
        //    }

        //    try
        //    {
        //        ctx.Adverts.Update(result);
        //        ctx.SaveChanges();

        //        return Ok("Adverted edited.");
        //    }
        //    catch (Exception e)
        //    {
        //        return StatusCode(500, "Failed to edit advert." + e.Message);
        //    }


        //}

        [HttpPost]
        [Route("edit_advert")]
        public ActionResult<string> EditAdvert(string editJson)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(unAuthMsg);

            Advert editAdvert = JsonSerializer.Deserialize<Advert>(editJson);
            Advert result = ctx.Adverts.Where(ad => editAdvert.Id == ad.Id).FirstOrDefault();

            if(result != null)
            {
                if (result.OwnerId != userId)
                    return BadRequest("Greška, niste vlasnik oglasa.");

                try
                {
                    PropertyInfo[] properties = typeof(Advert).GetProperties();

                    foreach (PropertyInfo property in properties)
                    {
                        if (property.Name == "Id" || property.Name == "OwnerId" || property.Name == "DateCreated")
                            continue;

                        property.SetValue(result, property.GetValue(editAdvert));
                    }

                    ctx.Adverts.Update(result);
                    ctx.SaveChanges();

                    return Ok("Oglas uspešno izmenjen.");
                }
                catch (Exception e)
                {
                    return StatusCode(500, "Greška pri izmeni podataka oglasa.");
                }
            }

            return NotFound("Oglas ne postoji.");
        }

        private bool IsFavourite(uint advertId)
        {
            var result = ctx.FavouriteAdverts.Where(f => f.AdvertId == advertId && f.UserId == userId).FirstOrDefault();

            if (result != null)
                return true;

            return false;
        }
    }
}