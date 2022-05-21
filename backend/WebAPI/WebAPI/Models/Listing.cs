using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Services;

namespace WebAPI.Models
{

    public static class Listing
    {
        public static SkuciSeImageService img = null;

        public static Func<Advert, object> AdListing =
            advert => new
            {
                Id = advert.Id,
                OwnerId = advert.OwnerId,
                Price = advert.Price,
                Title = advert.Title,
                City = advert.City,
                Address = advert.Address,
                Size = advert.Size,
                SaleType = advert.SaleType,
                ResidenceType = advert.ResidenceType,
                DateCreated = advert.DateCreated,
                Images = GetImages(advert.Id)
            };
        
        public static string[] GetImages(uint advertId)
        {
            if (img == null)
                return Array.Empty<string>();

            string dir = img.GetAdvertPath(advertId);
            if (Directory.Exists(dir))
            {
                var fullFiles = Directory.GetFiles(dir);
                List<string> files = new List<string>();

                foreach(var filePath in fullFiles)
                {
                    FileInfo fi = new FileInfo(filePath);
                    files.Add(fi.Name);
                }
                return files.ToArray();
            }
            else
                return Array.Empty<string>();
        }
    }
}