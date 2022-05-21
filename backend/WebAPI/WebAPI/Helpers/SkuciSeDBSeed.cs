using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Models;

namespace WebAPI.Helpers
{
    public static class SkuciSeDBSeed
    {
        public static void Seed(ModelBuilder modelBuilder, int seed)
        {
            Random rgen = new Random(seed);

            modelBuilder.Entity<User>().HasData(
                new User { Id = 1, Username = "test", Password = "test", Email = "test@mail.com", FirstName = "Pera", LastName = "Peric", DateCreated = DateTime.Now, Confirmed = true });

            modelBuilder.Entity<User>().HasData(
                new User { Id = 2, Username = "test2", Password = "test2", Email = "test2@mail.com", FirstName = "Mika", LastName = "Mikic", DateCreated = DateTime.Now, Confirmed = true });

            string[] names = new[]
            {
                "Stefan", "Vladimir", "Boris", "Stanislav", "Petar", "Aleksandar", "Mihajlo", "Milica", "Sara", "Aleksandra"
            };

            string[] lastNames = new[]
            {
                "Gabarević", "Gavranić", "Abramović", "Aksić", "Aleksić", "Kazimirović", "Ugrenović", "Miličević", "Babović", "Babić"
            };

            for(uint i=1; i<11; i++)
            {
                modelBuilder.Entity<User>().HasData(
                new User { 
                    Id = i+2, 
                    Username = $"rgen{i}", 
                    Password = $"Password{i}", 
                    Email = $"rgen{i}@hotmail.com", 
                    FirstName = PickOne(rgen, names),
                    LastName = PickOne(rgen, lastNames), 
                    DateCreated = DateTime.Now, 
                    Confirmed = true });
            }

            uint numApartments = 35;
            uint numHouses = 20;

            var trueOrFalse = new[] { true, false };
            var saleTypeOptions = new[] { SaleType.Purchase, SaleType.Rent };
            var structureTypeOptionsHouse = new[] { StructureType.OneRoom, StructureType.OneAndAHalfRoom, StructureType.TwoRoom, StructureType.TwoAndAHalfRoom };
            var structureTypeOptionsApartment = new[] { StructureType.Studio, StructureType.OneRoom, StructureType.OneAndAHalfRoom, StructureType.TwoRoom, StructureType.TwoAndAHalfRoom };

            Dictionary<StructureType, decimal> sizeByStructureType = new Dictionary<StructureType, decimal>
            {
                [StructureType.Studio] = 26.00M,
                [StructureType.OneRoom] = 30.00M,
                [StructureType.OneAndAHalfRoom] = 40.00M,
                [StructureType.TwoRoom] = 48.00M,
                [StructureType.TwoAndAHalfRoom] = 30.00M,
            };

            Dictionary<SaleType, decimal> priceBySaleType = new Dictionary<SaleType, decimal>
            {
                [SaleType.Purchase] = 1234.00M,
                [SaleType.Rent] = 9.00M,
            };

            var streetNames = new[]
            {
                "BANJIČKA",
                "BEOGRADSKOG BATALJONA",
                "MILIVOJA POPOVIĆA-MIĆE",
                "LAZARA JANKOVIĆA",
                "SEDMOG JULA",
                "BRÐANSKA",
                "STARA LIPOVICA",
                "PARTIZANSKA",
                "IZVORSKA",
                "DOBRIVOJA BANOVIĆA",
                "STRAHINJE PETROVIĆA",
                "DR BORISLAVA VUJADINOVIĆA",
                "STEFANA KNEŽEVIĆA",
                "PROTE JEFTIMIJA IVANOVIĆA",
                "STEVANA SIMIĆA"
            };

            Dictionary<SaleType, string> titleSaleType = new Dictionary<SaleType, string> { 
                [SaleType.Purchase] = "prodaju", 
                    [SaleType.Rent] = "najam" 
            };

            for (uint i = 1; i < numApartments + 1; i++)
            {
                var structureType = PickOne(rgen, structureTypeOptionsApartment);
                var saleType = PickOne(rgen, saleTypeOptions);
                var size = sizeByStructureType[structureType] + rgen.Next() % 6;

                var price = size * priceBySaleType[saleType];

                modelBuilder.Entity<Advert>().HasData(
                    new Advert
                    {
                        Id = i,
                        ResidenceType = ResidenceType.Apartment,
                        SaleType = saleType,
                        StructureType = structureType,
                        Title = $"Stan na {titleSaleType[saleType]}!",
                        Description = @"Opis oglasa. Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,",
                        City = $"{PickOne(rgen, SkuciSeDBContext.CityNames.ToArray())}",
                        Address = $"{PickOne(rgen, streetNames)} {(rgen.Next() % 30)}",
                        Size = size,
                        Price = price,
                        OwnerId = (uint)rgen.Next() % 10 + 1,
                        NumBedrooms = (uint)(rgen.Next() % 3) + 1,
                        NumBathrooms = (uint)(rgen.Next() % 2) + 1,
                        Furnished = PickOne(rgen, trueOrFalse),
                        YearOfMake = (uint)(1970 + (rgen.Next() % 50)),
                        DateCreated = DateTime.Now - new TimeSpan(days: rgen.Next() % 365, 0, 0, 0)
                    });
            }

            for (uint i = 1; i < numHouses + 1; i++)
            {
                var structureType = PickOne(rgen, structureTypeOptionsHouse);
                var saleType = PickOne(rgen, saleTypeOptions);
                var size = sizeByStructureType[structureType] + rgen.Next() % 6;

                var price = size * priceBySaleType[saleType];

                modelBuilder.Entity<Advert>().HasData(
                    new Advert
                    {
                        Id = i + numApartments,
                        ResidenceType = ResidenceType.House,
                        SaleType = saleType,
                        StructureType = structureType,
                        Title = $"Kuća na {titleSaleType[saleType]}!",
                        Description = @"Opis oglasa. Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,",
                        City = $"{PickOne(rgen, SkuciSeDBContext.CityNames.ToArray())}",
                        Address = $"{PickOne(rgen, streetNames)} {(rgen.Next() % 30)}",
                        Size = size * 3,
                        Price = price,
                        OwnerId = (uint)rgen.Next() % 2 + 1,
                        NumBedrooms = (uint)(rgen.Next() % 4) + 1,
                        NumBathrooms = (uint)(rgen.Next() % 2) + 1,
                        Furnished = PickOne(rgen, trueOrFalse),
                        YearOfMake = (uint)(1950 + (rgen.Next() % 70)),
                        DateCreated = DateTime.Now - new TimeSpan(days: rgen.Next() % 365, 0, 0, 0)
                    });
            }

            modelBuilder.Entity<Message>().HasData(
                    new Message
                    {
                        Id = 1,
                        SenderId = 1,
                        ReceiverId = 2,
                        Content = "Hello world!",
                        SendDate = DateTime.Now,
                        Seen = false
                    });

            for (uint i = 2; i <= 11; i++)
            {
                uint uId1 = (uint)rgen.Next() % 10 + 1;
                uint uId2 = (uint)rgen.Next() % 10 + 1;
                if (uId1 == uId2)
                    uId2 = (uId1 + 1) % 10 + 1;

                modelBuilder.Entity<Message>().HasData(
                    new Message
                    {
                        Id = i,
                        SenderId = uId1,
                        ReceiverId = uId2,
                        Content = "[Test poruka kreirana od strane servera]",
                        SendDate = DateTime.UnixEpoch + new TimeSpan(days: 365*45 + rgen.Next() % 2000, 0, 0, 0),
                        Seen = PickOne(rgen, trueOrFalse)
                    });
                modelBuilder.Entity<Message>().HasData(
                    new Message
                    {
                        Id = i + 10,
                        SenderId = uId2,
                        ReceiverId = uId1,
                        Content = "[Test poruka kreirana od strane servera]",
                        SendDate = DateTime.UnixEpoch + new TimeSpan(days: 365 * 45 + rgen.Next() % 2000, 0, 0, 0),
                        Seen = PickOne(rgen, trueOrFalse)
                    });
            }

            for (uint i = 2; i <= 5; i++)
            {
                modelBuilder.Entity<FavouriteAdvert>().HasData(
                    new FavouriteAdvert
                    {
                        UserId = 1,
                        AdvertId = i
                    }) ;

                modelBuilder.Entity<FavouriteAdvert>().HasData(
                    new FavouriteAdvert
                    {
                        UserId = 2,
                        AdvertId = i+10
                    });
            }


            string[] review = new string[] { "veoma loš", "loš", "ok", "dobar", "odličan" };

            for (uint i = 1; i < 41; i++)
            {
                bool concluded = PickOne(rgen, trueOrFalse);

                if (concluded)
                {
                    modelBuilder.Entity<Meeting>().HasData(
                        new Meeting
                        {
                            Id = i,
                            AdvertId = i,
                            VisitorId = (uint)rgen.Next()%10,
                            Time = DateTime.Now,
                            AgreedVisitor = true,
                            AgreedOwner = true,
                            DateCreated = DateTime.Now,
                            Concluded = true
                        });

                    uint rating = PickOne(rgen, new uint[] { 1, 2, 3, 4, 5 });

                    modelBuilder.Entity<Review>().HasData(
                        new Review
                        {
                            MeetingId = i,
                            Rating = rating,
                            Text = $"Moj utisak je {review[rating-1]}!"
                        });
                }
                else
                {
                    modelBuilder.Entity<Meeting>().HasData(
                        new Meeting
                        {
                            Id = i,
                            AdvertId = i,
                            VisitorId = (uint)rgen.Next() % 10,
                            Time = DateTime.Now + new TimeSpan(days: rgen.Next()%7, 0, 0, 0),
                            AgreedVisitor = true,
                            AgreedOwner = false,
                            DateCreated = DateTime.Now,
                            Concluded = false
                        });
                }
            }
        }

        private static T PickOne<T>(Random r, T[] options)
        {
            return options[r.Next() % options.Length];
        }
    }
}
